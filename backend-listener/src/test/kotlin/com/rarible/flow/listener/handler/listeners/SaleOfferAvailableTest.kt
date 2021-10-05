package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.BaseIntegrationTest
import com.rarible.flow.listener.IntegrationTest
import com.rarible.flow.listener.createItem
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime

@IntegrationTest
class SaleOfferAvailableTest : BaseIntegrationTest() {

    @Test
    fun `should handle order opened`() = runBlocking<Unit> {
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
            )
        ).forEach { eventHandler.handle(it) }

        orderRepository.coFindById(10859892) shouldNotBe null
    }

    @Test
    fun `should calculate order data correctly`() = runBlocking<Unit> {
        val orderData = SaleOfferAvailable.orderData(
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


    companion object {
        class PayoutMatcher(val address: FlowAddress, val amount: BigDecimal) : Matcher<Payout> {
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
