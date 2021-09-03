package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.createItem
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal
import java.time.LocalDateTime

internal class DepositListenerTest: FunSpec({

    val item = createItem()

    val itemService = mockk<ItemService>() {
        coEvery { transferNft(any(), any()) } answers {
            item.copy(owner = arg(1)) to Ownership(
                contract = item.contract,
                tokenId = item.tokenId,
                owner = arg(1),
                date = item.date
            )
        }

        coEvery { byId(any()) } returns item
    }
    val protocolEventPublisher = mockk<ProtocolEventPublisher>() {
        coEvery {
            onUpdate(any<Ownership>())
        } returns KafkaSendResult.Success("1")

        coEvery {
            onItemUpdate(any())
        } returns KafkaSendResult.Success("2")
    }
    val itemHistoryRepository = mockk<ItemHistoryRepository>() {
        every {
            save(any())
        } answers {
            Mono.just(arg(0))
        }
    }
    val orderRepository = mockk<OrderRepository>() {
        every { save(any()) } answers { Mono.just(arg(0)) }
        every { findByItemId(any()) } answers {
            Order(
                1L,
                item.id,
                FlowAddress("0x1000"),
                null,
                FlowAssetNFT(item.contract, 1.toBigDecimal(), item.tokenId),
                null,
                1.toBigDecimal(),
                item.id.toString(),
                data = OrderData(emptyList(), emptyList()),
                collection = item.collection,
                fill = BigDecimal.valueOf(3.5)
            ).toMono()
        }

    }

    val listener = DepositListener(
        itemService,
        protocolEventPublisher,
        itemHistoryRepository,
        orderRepository
    )

    val eventHandler = EventHandler(
        mapOf(
            DepositListener.ID to listener
        )
    )

    test("should handle deposit") {
        val event = EventMessage(
            EventId.of("A.fcfb23c627a63d40.CommonNFT.Deposit"),
            mapOf(
                "id" to "12",
                "to" to "0xfcfb23c627a63d40",
            ),
            LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
            BlockInfo(
                "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                40172320,
                "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
            )
        )

        eventHandler.handle(event)

        verify() {
            orderRepository.save(withArg {
                it.taker shouldBe  FlowAddress("0xfcfb23c627a63d40")
            })
        }
    }
})
