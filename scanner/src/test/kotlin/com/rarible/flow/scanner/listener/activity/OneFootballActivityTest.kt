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

internal class OneFootballActivityTest: FunSpec({

    val activityMaker = OneFootballActivity(mockk {
        every { chainId } returns FlowChainId.MAINNET
    })

    test("should mint item") {
        activityMaker.activities(
            listOf(
                MINT_LOG_EVENT
            )
        ) shouldContainValue MintActivity(
            owner = "0x6831760534292098",
            contract = "A.6831760534292098.OneFootballCollectible",
            tokenId = 1,
            creator = "0x6831760534292098",
            metadata = emptyMap(),
            royalties = listOf(
                Part(FlowAddress("0x6831760534292098"), 0.005)
            ),
            timestamp = Instant.parse("2021-10-26T14:28:35.621Z")
        )
    }

    test("tokenId") {
        activityMaker.tokenId(MINT_LOG_EVENT) shouldBe 1
    }

    test("meta") {
        activityMaker.meta(MINT_LOG_EVENT) shouldBe emptyMap()
    }

    test("contractName") {
        activityMaker.contractName shouldBe "OneFootballCollectible"
    }

    test("isSupportedCollection") {
        activityMaker.isSupportedCollection("A.6831760534292098.OneFootballCollectible")
    }

}) {
    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
                Log.Status.CONFIRMED,
                0,
                "A.6831760534292098.OneFootballCollectible.Minted",
                Instant.parse("2021-10-26T14:28:35.621Z"),
                19683033,
                "85992a4b68aae43d7743cc68c5bf622655242dd841a036910230ca29fa96da49"
            ),
            event = EventMessage(
                EventId.of("A.6831760534292098.OneFootballCollectible.Minted"),
                mapOf(
                    "id" to UInt64NumberField("1"),
                    "templateID" to UInt64NumberField("0")
                )
            ),
            type = FlowLogType.MINT,

        )
    }
}