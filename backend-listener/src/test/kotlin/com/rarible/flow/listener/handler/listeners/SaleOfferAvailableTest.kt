package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.createItem
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDateTime

internal class SaleOfferAvailableTest: FunSpec({

    val item = createItem()
    val order = Order(
        1L,
        item.id,
        FlowAddress("0x1000"),
        null,
        FlowAssetNFT(item.contract, 1.toBigDecimal(), item.tokenId),
        null,
        1.toBigDecimal(),
        item.id.toString(),
        buyerFee = BigDecimal.ZERO,
        sellerFee = BigDecimal.ZERO,
        data = OrderData(emptyList(), emptyList()),
        collection = item.collection
    )

    val itemRepository = mockk<ItemRepository>() {
        every { save(any()) } answers { Mono.just(arg(0)) }
        every { findById(any<ItemId>()) } returns Mono.just(item)
    }

    val orderRepository = mockk<OrderRepository>() {
        every { save(any()) } answers { Mono.just(arg(0)) }
    }

    val protocolEventPublisher = mockk<ProtocolEventPublisher>() {
        coEvery {
            onItemUpdate(any())
        } returns KafkaSendResult.Success("1")

        coEvery {
            onUpdate(any<Order>())
        } returns KafkaSendResult.Success("1")
    }
    val itemHistoryRepository = mockk<ItemHistoryRepository>() {
        every { save(any()) } answers { Mono.just(arg(0)) }
    }

    val listener = SaleOfferAvailable(
        itemRepository,
        orderRepository,
        protocolEventPublisher,
        itemHistoryRepository,
    )

    val eventHandler = EventHandler(
        mapOf(
            SaleOfferAvailable.ID to listener
        )
    )

    test("should handle order opened") {
        val event = EventMessage(
            EventId.of("A.fcfb23c627a63d40.NFTStorefront.SaleOfferAvailable"),
            mapOf(
                "id" to "10859892",
                "nftType" to "A.fcfb23c627a63d40.CommonNFT.NFT",
                "nftID" to "54",
                "bidType" to "A.7e60df042a9c0868.FlowToken.Vault",
                "price" to "10.12300000",
                "buyerFee" to "2.50000000",
                "sellerFee" to "2.50000000"
            ),
            LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
            BlockInfo(
                "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                40172320,
                "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
            )
        )

        eventHandler.handle(event)


    }

    test("should calculate order data correctly") {
        val orderData = listener.orderData(
            200.toBigDecimal(),
            mockk {
                every { royalties } answers {
                    listOf(
                        Part(FlowAddress("0x01"), 1.0),
                        Part(FlowAddress("0x02"), 2.0),
                        Part(FlowAddress("0x03"), 3.0),
                    )
                }

                every {
                    owner
                } answers { FlowAddress("0x1111") }
            }
        )

        orderData.originalFees should {
            it shouldHaveSize 3

            it[0] should PayoutMatcher(FlowAddress("0x01"), 1.toBigDecimal())
            it[1] should PayoutMatcher(FlowAddress("0x02"), 2.toBigDecimal())
            it[2] should PayoutMatcher(FlowAddress("0x03"), 3.toBigDecimal())

        }

        orderData.payouts should {
            it shouldHaveSize 4

            it[0] should PayoutMatcher(FlowAddress("0x01"), 2.toBigDecimal())
            it[1] should PayoutMatcher(FlowAddress("0x02"), 4.toBigDecimal())
            it[2] should PayoutMatcher(FlowAddress("0x03"), 6.toBigDecimal())
            it[3] should PayoutMatcher(FlowAddress("0x1111"), 188.toBigDecimal())
        }


    }

}) {
    companion object {
        class PayoutMatcher(val address: FlowAddress, val amount: BigDecimal): Matcher<Payout>{
            override fun test(value: Payout): MatcherResult {
                return MatcherResult.invoke(
                    value.account == address && value.value.toDouble() == amount.toDouble(),
                    { "Expected Payout(${address.formatted}, $amount), got: $value" },
                    { "" }
                )

            }
        }
    }
}
