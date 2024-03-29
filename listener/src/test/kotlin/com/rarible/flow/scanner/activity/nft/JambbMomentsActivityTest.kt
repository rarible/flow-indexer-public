package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.activity.disabled.JambbMomentsActivity
import io.kotest.matchers.maps.shouldContainValue
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant

internal class JambbMomentsActivityTest : AbstractNftActivityTest() {
    val activityMaker = JambbMomentsActivity(logRepository, txManager, chainId)

    @Test
    fun `mint item - ok`() = runBlocking<Unit> {
        activityMaker.activities(
            listOf(
                MINT_LOG_EVENT
            )
        ) shouldContainValue MintActivity(
            owner = "0xd4ad4740ee426334",
            contract = "A.d4ad4740ee426334.Moments",
            tokenId = 10,
            creator = "0xd4ad4740ee426334",
            metadata = mapOf(
                "momentID" to "10",
                "contentID" to "11",
                "contentEditionID" to "12",
                "serialNumber" to "13",
                "seriesID" to "14",
                "setID" to "15",
            ),
            royalties = listOf(
                Part(FlowAddress("0x609a2ea0548b4b51"), 0.05)
            ),
            timestamp = Instant.parse("2021-10-26T14:28:35.621Z"),
            collection = "A.d4ad4740ee426334.Moments"
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
        activityMaker.tokenId(MINT_LOG_EVENT) shouldBe 10
    }

    @Test
    fun contractName() {
        activityMaker.contractName shouldBe "Moments"
    }

    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
                0,
                "A.d4ad4740ee426334.Moments.MomentMinted",
                Instant.parse("2021-10-26T14:28:35.621Z"),
                19683033,
                "85992a4b68aae43d7743cc68c5bf622655242dd841a036910230ca29fa96da49"
            ),
            event = EventMessage(
                EventId.of("A.d4ad4740ee426334.Moments.MomentMinted"),
                mapOf(
                    "momentID" to UInt64NumberField("10"),
                    "contentID" to UInt64NumberField("11"),
                    "contentEditionID" to UInt64NumberField("12"),
                    "serialNumber" to UInt64NumberField("13"),
                    "seriesID" to UInt64NumberField("14"),
                    "setID" to UInt64NumberField("15"),
                )
            ),
            type = FlowLogType.MINT
        )
    }
}
