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
import com.rarible.flow.scanner.activity.disabled.CnnActivity
import io.kotest.matchers.maps.shouldContainValue
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant

internal class CnnActivityTest : AbstractNftActivityTest() {
    val activityMaker = CnnActivity(logRepository, txManager, properties)

    @Test
    fun `should mint ite`() = runBlocking<Unit> {
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
        activityMaker.tokenId(MINT_LOG_EVENT) shouldBe 11
    }

    @Test
    fun meta() {
        activityMaker.meta(MINT_LOG_EVENT) shouldBe emptyMap()
    }

    @Test
    fun contractName() {
        activityMaker.contractName shouldBe "CNN_NFT"
    }

    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
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
