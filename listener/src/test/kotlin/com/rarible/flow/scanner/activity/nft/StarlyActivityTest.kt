package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.StringField
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.activity.disabled.StarlyActivity
import io.kotest.matchers.maps.shouldContainValue
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant

internal class StarlyActivityTest : AbstractNftActivityTest() {

    val activityMaker = StarlyActivity(logRepository, txManager, properties)

    @Test
    fun `mint item - ok`() = runBlocking<Unit> {
        activityMaker.activities(
            listOf(
                MINT_LOG_EVENT
            )
        ) shouldContainValue MintActivity(
            owner = "0x5b82f21c0edf76e3",
            contract = "A.5b82f21c0edf76e3.StarlyCard",
            tokenId = 8322,
            creator = "0x5b82f21c0edf76e3",
            metadata = mapOf(
                "starlyId" to "pSYegq3aubUCodcy1t4u/15/542"
            ),
            royalties = listOf(
                Part(FlowAddress("0x12c122ca9266c278"), 0.1)
            ),
            timestamp = Instant.parse("2021-10-26T14:28:35.621Z"),
            collection = "A.5b82f21c0edf76e3.StarlyCard"
        )
        coVerify {
            logRepository.findAfterEventIndex(
                eq(MotoGPActivityMakerTest.MINT_LOG_EVENT.log.transactionHash),
                eq(MotoGPActivityMakerTest.MINT_LOG_EVENT.log.eventIndex),
                any(),
                any()
            )
        }
    }

    @Test
    fun tokenId() {
        activityMaker.tokenId(MINT_LOG_EVENT) shouldBe 8322
    }

    @Test
    fun meta() {
        activityMaker.meta(MINT_LOG_EVENT) shouldBe mapOf(
            "starlyId" to "pSYegq3aubUCodcy1t4u/15/542"
        )
    }

    @Test
    fun contractName() {
        activityMaker.contractName shouldBe "StarlyCard"
    }

    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
                0,
                "A.5b82f21c0edf76e3.StarlyCard.Minted",
                Instant.parse("2021-10-26T14:28:35.621Z"),
                19683033,
                "85992a4b68aae43d7743cc68c5bf622655242dd841a036910230ca29fa96da49"
            ),
            event = EventMessage(
                EventId.of("A.5b82f21c0edf76e3.StarlyCard.Minted"),
                mapOf(
                    "id" to UInt64NumberField("8322"),
                    "starlyID" to StringField("pSYegq3aubUCodcy1t4u/15/542")
                )
            ),
            type = FlowLogType.MINT,

        )
    }
}
