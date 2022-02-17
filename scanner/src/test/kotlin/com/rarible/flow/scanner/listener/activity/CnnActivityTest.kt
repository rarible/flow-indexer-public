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

internal class CnnActivityTest : FunSpec({
    val activityMaker = CnnActivity(mockk {
        every { chainId } returns FlowChainId.MAINNET
    })

    test("should mint item") {
        activityMaker.activities(
            listOf(
                MINT_LOG_EVENT
            )
        ) shouldContainValue MintActivity(
            owner = "0x329feb3ab062d289",
            contract = "A.329feb3ab062d289.CNN_NFT",
            tokenId = 11,
            creator = "0x329feb3ab062d289",
            metadata = emptyMap(),
            royalties = listOf(
                Part(FlowAddress("0x55c8be371f74168f"), 0.1)
            ),
            timestamp = Instant.parse("2021-10-26T14:28:35.621Z"),
            collection = "A.329feb3ab062d289.CNN_NFT"
        )
    }

    test("tokenId") {
        activityMaker.tokenId(MINT_LOG_EVENT) shouldBe 11
    }

    test("meta") {
        activityMaker.meta(MINT_LOG_EVENT) shouldBe emptyMap()
    }

    test("contractName") {
        activityMaker.contractName shouldBe "CNN_NFT"
    }

}) {
    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
                Log.Status.CONFIRMED,
                0,
                "A.329feb3ab062d289.CNN_NFT.Minted",
                Instant.parse("2021-10-26T14:28:35.621Z"),
                19683033,
                "85992a4b68aae43d7743cc68c5bf622655242dd841a036910230ca29fa96da49"
            ),
            event = EventMessage(
                EventId.of("A.329feb3ab062d289.CNN_NFT.Minted"),
                mapOf(
                    "id" to UInt64NumberField("11"),
                )
            ),
            type = FlowLogType.MINT
        )
    }
}
