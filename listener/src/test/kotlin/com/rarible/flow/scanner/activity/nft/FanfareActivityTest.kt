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
import com.rarible.flow.scanner.activity.disabled.FanfareActivity
import io.kotest.matchers.maps.shouldContainValue
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant

internal class FanfareActivityTest : AbstractNftActivityTest() {

    val activityMaker = FanfareActivity(logRepository, txManager, chainId)

    @Test
    fun `mint item - ok`() = runBlocking<Unit> {
        activityMaker.activities(
            listOf(
                MINT_LOG_EVENT
            )
        ) shouldContainValue MintActivity(
            owner = "0x4c44f3b1e4e70b20",
            contract = "A.4c44f3b1e4e70b20.FanfareNFTContract",
            tokenId = 1337,
            creator = "0x4c44f3b1e4e70b20",
            metadata = mapOf(
                "metadata" to "SOME_BIG_JSON"
            ),
            royalties = listOf(
                Part(FlowAddress("0xa161c109f0902908"), 0.15)
            ),
            timestamp = Instant.parse("2021-10-26T14:28:35.621Z"),
            collection = "A.4c44f3b1e4e70b20.FanfareNFTContract"
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
            "metadata" to "SOME_BIG_JSON"
        )
    }

    @Test
    fun contractName() {
        activityMaker.contractName shouldBe "FanfareNFTContract"
    }

    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
                0,
                "A.4c44f3b1e4e70b20.FanfareNFTContract.Minted",
                Instant.parse("2021-10-26T14:28:35.621Z"),
                19683033,
                "85992a4b68aae43d7743cc68c5bf622655242dd841a036910230ca29fa96da49"
            ),
            event = EventMessage(
                EventId.of("A.4c44f3b1e4e70b20.FanfareNFTContract.Minted"),
                mapOf(
                    "id" to UInt64NumberField("1337"),
                    "metadata" to StringField(
                        """
                        SOME_BIG_JSON
                        """.trimIndent()
                    )
                )
            ),
            type = FlowLogType.MINT,

        )
    }
}
