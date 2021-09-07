package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.BaseIntegrationTest
import com.rarible.flow.listener.IntegrationTest
import com.rarible.flow.listener.createItem
import com.rarible.flow.listener.handler.EventHandler
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

@IntegrationTest
internal class OrderClosedListenerTest: BaseIntegrationTest() {

    val item = createItem()
    val order = Order(
        1L,
        item.id,
        FlowAddress("0x1000"),
        FlowAddress("0x1001"),
        FlowAssetNFT(item.contract, 1.toBigDecimal(), item.tokenId),
        FlowAssetFungible("0x1234", 10000.toBigDecimal()),
        1.toBigDecimal(),
        item.id.toString(),
        data = OrderData(emptyList(), emptyList()),
        collection = "ABC"
    )

    @Test
    fun `should handle buy event`() = runBlocking<Unit> {
        listOf(
            EventMessage(
                EventId.of("A.fcfb23c627a63d40.CommonNFT.Mint"),
                mapOf(
                    "id" to "12",
                    "collection" to "A.fcfb23c627a63d40.CommonNFT.NFT",
                    "creator" to "0x01",
                    "metadata" to "url://",
                    "royalties" to listOf(
                        mapOf("address" to "0x2ec081d566da0184", "fee" to "25.00000000"),
                        mapOf("address" to "0xe91e497115b9731b", "fee" to "5.00000000"),
                    )
                ),
                LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
                BlockInfo(
                    "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                    40172320,
                    "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
                )
            ),

            EventMessage(
                EventId.of("A.fcfb23c627a63d40.NFTStorefront.SaleOfferAvailable"),
                mapOf(
                    "saleOfferResourceID" to "10859892",
                    "nftType" to "A.fcfb23c627a63d40.CommonNFT.NFT",
                    "nftID" to "12",
                    "ftVaultType" to "A.7e60df042a9c0868.FlowToken.Vault",
                    "price" to "10.12300000"
                ),
                LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
                BlockInfo(
                    "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                    40172320,
                    "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
                )
            ),
            EventMessage(
                EventId.of("A.fcfb23c627a63d40.NFTStorefront.SaleOfferCompleted"),
                mapOf(
                    "saleOfferResourceID" to "10859892",
                    "accepted" to "true"
                ),
                LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
                BlockInfo(
                    "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                    40172320,
                    "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
                )
            )
        ).forEach {
            eventHandler.handle(it)
        }

        orderRepository.coFindById(10859892) should { order ->
            order as Order
            order.fill shouldBe BigDecimal("10.12300000")
        }

    }
}
