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
import com.rarible.flow.scanner.activity.disabled.ChainMonstersActivity
import io.kotest.matchers.maps.shouldContainValue
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant

internal class ChainMonstersActivityTest : AbstractNftActivityTest() {
    val activityMaker = ChainMonstersActivity(logRepository, txManager, chainId)

    @Test
    fun `should mint item`() = runBlocking<Unit> {
        activityMaker.activities(
            listOf(
                MINT_LOG_EVENT
            )
        ) shouldContainValue MintActivity(
            owner = "0x93615d25d14fa337",
            contract = "A.93615d25d14fa337.ChainmonstersRewards",
            tokenId = 11,
            creator = "0x93615d25d14fa337",
            metadata = mapOf(
                "rewardId" to "1000",
                "serialNumber" to "33"
            ),
            royalties = listOf(
                Part(FlowAddress("0x64f83c60989ce555"), 0.05)
            ),
            timestamp = Instant.parse("2021-10-26T14:28:35.621Z"),
            collection = "A.93615d25d14fa337.ChainmonstersRewards"
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
        activityMaker.meta(MINT_LOG_EVENT) shouldBe mapOf(
            "rewardId" to "1000",
            "serialNumber" to "33"
        )
    }

    @Test
    fun contractName() {
        activityMaker.contractName shouldBe "ChainmonstersRewards"
    }

    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
                0,
                "A.93615d25d14fa337.ChainmonstersRewards.NFTMinted",
                Instant.parse("2021-10-26T14:28:35.621Z"),
                19683033,
                "85992a4b68aae43d7743cc68c5bf622655242dd841a036910230ca29fa96da49"
            ),
            event = EventMessage(
                EventId.of("A.93615d25d14fa337.ChainmonstersRewards.NFTMinted"),
                mapOf(
                    "NFTID" to UInt64NumberField("11"),
                    "rewardID" to UInt32NumberField("1000"),
                    "serialNumber" to UInt32NumberField("33")
                )
            ),
            type = FlowLogType.MINT
        )
    }
}
