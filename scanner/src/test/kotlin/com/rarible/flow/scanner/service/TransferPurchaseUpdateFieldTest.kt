package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderActivityMatchSide
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.domain.OrderType
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.scanner.eventprocessor.ItemIndexerEventProcessor
import com.rarible.flow.scanner.eventprocessor.OrderIndexerEventProcessor
import com.rarible.flow.scanner.model.IndexerEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class TransferPurchaseUpdateFieldTest : FunSpec({

    val timestamp = Instant.now()
    val itemId = ItemId("A.0000000000000000.Market", 42L)
    val assetNFT = FlowAssetNFT("A.0000000000000000.NFT", BigDecimal.ONE, itemId.tokenId)
    val assetFungible = FlowAssetFungible("A.0000000000000000.Token", BigDecimal.ONE)
    val maker = "0x0000000000000001"
    val taker = "0x0000000000000002"
    val tx1Hash = "0000"
    val tx2Hash = "9999"
    val takeSide = OrderActivityMatchSide(maker, assetNFT)
    val makeSide = OrderActivityMatchSide(taker, assetFungible)

    val sellActivity = FlowNftOrderActivitySell(
        price = BigDecimal.ONE,
        priceUsd = BigDecimal.ONE,
        contract = itemId.contract,
        tokenId = itemId.tokenId,
        timestamp = timestamp,
        hash = "4242",
        left = takeSide,
        right = makeSide,
    )

    val bidActivity = FlowNftOrderActivitySell(
        price = BigDecimal.ONE,
        priceUsd = BigDecimal.ONE,
        contract = itemId.contract,
        tokenId = itemId.tokenId,
        timestamp = timestamp,
        hash = "4242",
        left = makeSide,
        right = takeSide,
    )

    val order = Order(
        id = 42L,
        itemId = itemId,
        maker = FlowAddress(maker),
        make = takeSide.asset,
        take = makeSide.asset,
        amount = BigDecimal.ONE,
        createdAt = LocalDateTime.now(),
        collection = itemId.contract,
        makeStock = assetNFT.value,
        lastUpdatedAt = LocalDateTime.now(),
        type = OrderType.LIST,
        takePriceUsd = BigDecimal.ONE,
        status = OrderStatus.ACTIVE,
    )

    val transferActivity = TransferActivity(
        contract = itemId.contract,
        tokenId = itemId.tokenId,
        timestamp = timestamp,
        from = maker,
        to = taker,
        purchased = false,
    )

    val log = FlowLog(
        transactionHash = tx1Hash,
        status = Log.Status.CONFIRMED,
        eventIndex = 0,
        eventType = "Any",
        timestamp = timestamp,
        blockHash = "1111",
        blockHeight = 0L,
    )

    val anotherLog = log.copy(transactionHash = tx2Hash)

    val historyOrderClose = ItemHistory(timestamp, sellActivity, log.copy(eventIndex = 1))
    val historyAcceptBid = ItemHistory(timestamp, bidActivity, log.copy(eventIndex = 1))
    val historyTransfer = ItemHistory(timestamp, transferActivity, log.copy(eventIndex = 2))

    val protocolEventPublisher = mockk<ProtocolEventPublisher> {
        coEvery { onOrderUpdate(any(), any()) } answers { KafkaSendResult.Success(firstArg<Order>().id.toString()) }
        coEvery { onItemUpdate(any()) } answers { KafkaSendResult.Success(firstArg<Item>().id.toString()) }
        coEvery { onUpdate(any()) } answers { KafkaSendResult.Success(firstArg<Ownership>().id.toString()) }
        coEvery { activity(any()) } answers { KafkaSendResult.Success(firstArg<ItemHistory>().id) }
    }

    @Suppress("ReactiveStreamsUnusedPublisher")
    val orderRepository = mockk<OrderRepository> {
        every { findById(any<Long>()) } returns Mono.just(order)
        every {
            findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(any(), any(), any(), any())
        } returns Flux.empty()
        every { save(any()) } answers { Mono.just(firstArg()) }
    }

    @Suppress("ReactiveStreamsUnusedPublisher")
    val itemHistoryRepository = mockk<ItemHistoryRepository> {
        every { findTransferInTx(tx1Hash, maker, taker) } returns Flux.just(historyTransfer)
        every { findTransferInTx(tx2Hash, maker, taker) } returns Flux.empty()
        every { findOrderInTx(tx1Hash, maker, taker) } returns Flux.just(historyOrderClose)
        every { findOrderInTx(tx2Hash, maker, taker) } returns Flux.empty()
        every { save(any()) } answers { Mono.just(firstArg()) }
    }

    @Suppress("ReactiveStreamsUnusedPublisher")
    val ownershipRepository = mockk<OwnershipRepository> {
        every { findById(any<OwnershipId>()) } returns Mono.empty()
        every { save(any()) } answers { Mono.just(firstArg()) }
    }

    @Suppress("ReactiveStreamsUnusedPublisher")
    val itemRepository = mockk<ItemRepository> {
        every { save(any()) } answers { Mono.just(firstArg()) }
    }

    val orderService = OrderService(
        orderRepository,
        itemHistoryRepository,
        protocolEventPublisher,
        mockk(),
        mockk(),
    )

    val orderIndexerEventProcessor = OrderIndexerEventProcessor(
        orderService,
        protocolEventPublisher,
        mockk(),
    )

    val itemIndexerEventProcessor = ItemIndexerEventProcessor(
        itemRepository,
        mockk(),
        ownershipRepository,
        protocolEventPublisher,
        orderService,
    )

    val indexerEventService = IndexerEventService(listOf(itemIndexerEventProcessor, orderIndexerEventProcessor))

    beforeTest {
        println("before each clean")
        clearMocks(protocolEventPublisher, answers = false)
    }

    test("on order close: update transport activity") {
        val event = IndexerEvent(historyOrderClose, Source.BLOCKCHAIN, null)

        indexerEventService.processEvent(event)

        val historySlot = slot<ItemHistory>()
        coVerify {
            protocolEventPublisher.activity(capture(historySlot))
        }

        historySlot.isCaptured shouldBe true
        (historySlot.captured.activity as? TransferActivity)?.let {
            it.purchased shouldBe true
        } shouldNotBe null
    }

    test("on order close event: not found transfer") {
        val event = IndexerEvent(historyOrderClose.copy(log = anotherLog), Source.BLOCKCHAIN, null)

        indexerEventService.processEvent(event)

        coVerify {
            protocolEventPublisher.activity(any()) wasNot Called
        }
    }

    test("on bid accept: update transport activity") {
        val event = IndexerEvent(historyAcceptBid, Source.BLOCKCHAIN, null)

        indexerEventService.processEvent(event)

        val historySlot = slot<ItemHistory>()
        coVerify { protocolEventPublisher.activity(capture(historySlot)) }

        historySlot.isCaptured shouldBe true
        (historySlot.captured.activity as? TransferActivity)?.let {
            it.purchased shouldBe true
        } shouldNotBe null
    }

    test("on bid accept: not found transfer") {
        val event = IndexerEvent(historyAcceptBid, Source.BLOCKCHAIN, null)

        indexerEventService.processEvent(event)

        coVerify {
            protocolEventPublisher.activity(any()) wasNot Called
        }
    }

    test("on transport: found order (must be purchased=true)") {
        val event = IndexerEvent(historyTransfer, Source.BLOCKCHAIN, null)

        indexerEventService.processEvent(event)

        val historySlot = slot<ItemHistory>()
        coVerify { protocolEventPublisher.activity(capture(historySlot)) }

        historySlot.isCaptured shouldBe true
        (historySlot.captured.activity as? TransferActivity)?.let {
            it.purchased shouldBe true
        } shouldNotBe null
    }

    test("on transport: not found order") {
        val event = IndexerEvent(historyTransfer, Source.BLOCKCHAIN, null)

        indexerEventService.processEvent(event)

        coVerify {
            protocolEventPublisher.activity(any()) wasNot Called
        }
    }
})
