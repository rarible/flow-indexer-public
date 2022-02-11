package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainValue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

internal class JambbMomentsActivityTest : FunSpec({
    val activityMaker = JambbMomentsActivity(mockk {
        every { chainId } returns FlowChainId.MAINNET
    })

    test("should mint item") {
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
            timestamp = Instant.parse("2021-10-26T14:28:35.621Z")
        )
    }

    test("tokenId") {
        activityMaker.tokenId(MINT_LOG_EVENT) shouldBe 10
    }

    test("contractName") {
        activityMaker.contractName shouldBe "Moments"
    }

}) {
    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
                Log.Status.CONFIRMED,
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