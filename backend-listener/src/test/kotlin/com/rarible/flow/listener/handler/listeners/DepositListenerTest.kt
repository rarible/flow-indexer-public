package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.coFindAll
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.BaseIntegrationTest
import com.rarible.flow.listener.IntegrationTest
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@IntegrationTest
class DepositListenerTest() : BaseIntegrationTest() {

    @Test
    fun `should handle mint and deposit`() = runBlocking<Unit> {

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
                EventId.of("A.fcfb23c627a63d40.CommonNFT.Deposit"),
                mapOf(
                    "id" to "12",
                    "to" to "0x02",
                ),
                LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
                BlockInfo(
                    "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                    40172320,
                    "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
                )
            )
        ).forEach { eventHandler.handle(it) }

        val contract = "A.fcfb23c627a63d40.CommonNFT"
        val creator = FlowAddress("0x01")
        val owner = FlowAddress("0x02")
        itemRepository.coFindById(ItemId(contract, 12L)) shouldNotBe null

        ownershipRepository.coFindById(OwnershipId(contract, 12L, owner)) shouldNotBe null

        val history = itemHistoryRepository.coFindAll().toList().sortedBy { it.date }
        history shouldHaveSize 2
        val mint = history[0].activity as MintActivity
        mint.type shouldBe FlowActivityType.MINT
        mint.owner shouldBe creator
        mint.tokenId shouldBe 12L

        val transfer = history[1].activity as TransferActivity
        transfer.from shouldBe creator
        transfer.owner shouldBe owner

        itemEvents.receiveManualAcknowledge().take(2).toList()

        ownershipEvents.receiveManualAcknowledge().take(2).toList()
    }

    @Test
    fun `should handle sell and deposit`() = runBlocking<Unit> {

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
            ),

            EventMessage(
                EventId.of("A.fcfb23c627a63d40.CommonNFT.Deposit"),
                mapOf(
                    "id" to "12",
                    "to" to "0x02",
                ),
                LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
                BlockInfo(
                    "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                    40172320,
                    "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
                )
            )
        ).forEach { eventHandler.handle(it) }

        val contract = "A.fcfb23c627a63d40.CommonNFT"
        val creator = FlowAddress("0x01")
        val owner = FlowAddress("0x02")
        itemRepository.coFindById(ItemId(contract, 12L)) shouldNotBe null

        ownershipRepository.coFindById(OwnershipId(contract, 12L, owner)) shouldNotBe null

        val history = itemHistoryRepository.coFindAll().toList().sortedBy { it.date }
        history shouldHaveSize 3
        val mint = history[0].activity as MintActivity
        mint.type shouldBe FlowActivityType.MINT
        mint.owner shouldBe creator
        mint.tokenId shouldBe 12L

        val sell = history[2].activity as FlowNftOrderActivitySell
        sell shouldNotBe null

        itemEvents.receiveManualAcknowledge().take(2).toList()

        ownershipEvents.receiveManualAcknowledge().take(2).toList()
    }
}
