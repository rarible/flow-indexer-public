package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.BaseIntegrationTest
import com.rarible.flow.listener.IntegrationTest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@IntegrationTest
internal class SaleOfferCutTest: BaseIntegrationTest() {

    @Test
    fun `should handle non-existing order`() = runBlocking<Unit> {
        EventMessage(
            EventId.of("A.fcfb23c627a63d40.NFTStorefront.SaleOfferCut"),
            mapOf(
                "storefrontAddress" to "0x01",
                "saleOfferResourceID" to "1",
                "address" to "0x02",
                "amount" to "42"
            ),
            LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
            BlockInfo(
                "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                40172320,
                "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
            )
        ).also {
            eventHandler.handle(it)
        }
    }

    @Test
    fun `should update order data with origin fee`() {
        SaleOfferCut.updateOrderData(
            emptyList(),
            OrderData(emptyList(), emptyList()),
            SaleOfferCut.Companion.SaleOfferCutEvent(
                mapOf(
                    "storefrontAddress" to "0x01",
                    "saleOfferResourceID" to "1",
                    "address" to "0x02",
                    "amount" to "42"
                )
            )
        ) shouldBe OrderData(
            listOf(Payout(FlowAddress("0x02"), 42.toBigDecimal())),
            listOf(Payout(FlowAddress("0x02"), 42.toBigDecimal()))
        )
    }

    @Test
    fun `should update order data without origin fee`() {
        SaleOfferCut.updateOrderData(
            listOf(Part(FlowAddress("0x02"), 42.0)),
            OrderData(emptyList(), emptyList()),
            SaleOfferCut.Companion.SaleOfferCutEvent(
                mapOf(
                    "storefrontAddress" to "0x01",
                    "saleOfferResourceID" to "1",
                    "address" to "0x02",
                    "amount" to "42"
                )
            )
        ) shouldBe OrderData(
            listOf(Payout(FlowAddress("0x02"), 42.toBigDecimal())),
            emptyList()
        )
    }

}