package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.BaseIntegrationTest
import com.rarible.flow.listener.IntegrationTest
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime

@IntegrationTest
class MintListenerTest() : BaseIntegrationTest() {

    @Test
    fun `should handle mint`() = runBlocking<Unit> {
        val contract = "A.fcfb23c627a63d40.CommonNFT"
        val creator = "0xfcfb23c627a63d40"

        val event = EventMessage(
            EventId.of("A.fcfb23c627a63d40.CommonNFT.Mint"),
            mapOf(
                "id" to "12",
                "collection" to "A.fcfb23c627a63d40.CommonNFT.NFT",
                "creator" to creator,
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
        )

        eventHandler.handle(event)

        itemRepository.coFindById(ItemId(contract, 12L)) should { item ->
            item as Item
            item.creator shouldBe FlowAddress(creator)
            item.royalties shouldContainAll listOf(
                Part(FlowAddress("0x2ec081d566da0184"), 25.0),
                Part(FlowAddress("0xe91e497115b9731b"), 5.0),
            )
            item.owner shouldBe FlowAddress(creator)
            item.listed shouldBe false
            item.collection shouldBe contract
        }

        ownershipRepository.coFindById(
            OwnershipId(
                contract,
                12L,
                FlowAddress(creator)
            )
        ) should { ownership ->
            ownership shouldNotBe null
            ownership as Ownership
            ownership.contract shouldBe contract
            ownership.tokenId shouldBe 12L
            ownership.creators shouldContain Payout(FlowAddress(creator), BigDecimal.ONE)
        }

        val history = itemHistoryRepository.coFindAll().toList()
        history shouldHaveSize 1
        val activity = history[0].activity as MintActivity
        activity.type shouldBe FlowActivityType.MINT
        activity.owner shouldBe FlowAddress(creator)
        activity.tokenId shouldBe 12L

        val itemUpdates = async {
            itemEvents.receiveManualAcknowledge().first()
        }

        val protocolEvent = itemUpdates.await().value as FlowNftItemUpdateEventDto
        protocolEvent.item.id shouldBe "$contract:12"

        val ownershipUpdates = async {
            ownershipEvents.receiveManualAcknowledge().first()
        }.await()

        ownershipUpdates.value.ownershipId shouldBe "$contract:12:$creator"
    }

}
