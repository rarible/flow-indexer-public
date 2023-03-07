package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.UInt32NumberField
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.activity.disabled.EvolutionActivityMaker
import io.kotest.matchers.maps.shouldContainValue
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant

internal class EvolutionActivityMakerTest: AbstractNftActivityTest() {
    val activityMaker = EvolutionActivityMaker(logRepository, txManager, properties)

    @Test
    fun `should mint item`() = runBlocking<Unit> {
        activityMaker.activities(
            listOf(
                MINT_LOG_EVENT
            )
        ) shouldContainValue MintActivity(
            owner = "0xf4264ac8f3256818",
            contract = "A.f4264ac8f3256818.Evolution",
            tokenId = 1337,
            creator = "0xf4264ac8f3256818",
            metadata = mapOf(
                "itemId" to "1",
                "setId" to "3",
                "serialNumber" to "5"
            ),
            royalties = listOf(
                Part(FlowAddress("0x77b78d7d3f0d1787"), 0.1)
            ),
            timestamp = Instant.parse("2021-10-26T14:28:35.621Z"),
            collection = "A.f4264ac8f3256818.Evolution"
        )
        coVerify {
            logRepository.findAfterEventIndex(
                eq(MINT_LOG_EVENT.log.transactionHash),
                eq(MINT_LOG_EVENT.log.eventIndex),
                any(),
                any()
            )
        }
    }

    @Test
    fun tokenId() {
        activityMaker.tokenId(MINT_LOG_EVENT) shouldBe 1337
    }

    @Test
    fun meta() {
        activityMaker.meta(MINT_LOG_EVENT) shouldBe mapOf(
            "itemId" to "1",
            "setId" to "3",
            "serialNumber" to "5"
        )
    }

    @Test
    fun testContractName() {
        activityMaker.contractName shouldBe "Evolution"
    }

    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
                1337,
                "A.f4264ac8f3256818.Evolution.CollectibleMinted",
                Instant.parse("2021-10-26T14:28:35.621Z"),
                19683033,
                "85992a4b68aae43d7743cc68c5bf622655242dd841a036910230ca29fa96da49"
            ),
            event = EventMessage(
                EventId.of("A.f4264ac8f3256818.Evolution.CollectibleMinted"),
                mapOf(
                    "id" to UInt64NumberField("1337"),
                    "itemId" to UInt32NumberField("1"),
                    "setId" to UInt32NumberField("3"),
                    "serialNumber" to UInt32NumberField("5"),
                )
            ),
            type = FlowLogType.MINT,

            )
    }
}
