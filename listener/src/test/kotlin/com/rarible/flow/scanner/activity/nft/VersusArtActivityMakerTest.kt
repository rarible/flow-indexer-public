package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.CompositeAttribute
import com.nftco.flow.sdk.cadence.CompositeValue
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.StringField
import com.nftco.flow.sdk.cadence.StructField
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.activity.disabled.VersusArtActivityMaker
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class VersusArtActivityMakerTest : AbstractNftActivityTest() {
    private val activityMaker = VersusArtActivityMaker(logRepository, txManager, chainId)

    private val versusMint = FlowLogEvent(
        type = FlowLogType.MINT,
        log = FlowLog(
            transactionHash = "a2605f244d08395829a360a248249b7d47f8fbd04172f0ce4e82d4f7c25553a0",
            eventIndex = 2,
            eventType = "A.d796ff17107bbff6.Art.Created",
            timestamp = ZonedDateTime.parse("2021-04-29T08:37:00.545Z").toInstant(),
            blockHeight = 13969953,
            blockHash = "f3adc60e9e638cff9a921ada7cb1e97ce170b9ff908f33d4f6bab332f6c4ddcf",
        ),
        event = EventMessage(
            eventId = EventId(
                type = "A",
                contractAddress = FlowAddress("0xd796ff17107bbff6"),
                contractName = "Art",
                eventName = "Created"
            ),
            fields = mapOf(
                "id" to UInt64NumberField("16"),
                "metadata" to StructField(
                    CompositeValue(
                        id = "A.d796ff17107bbff6.Art.Metadata",
                        fields = arrayOf(
                            CompositeAttribute("name", StringField("Transcendence")),
                            CompositeAttribute("artist", StringField("ekaitza")),
                            CompositeAttribute("artistAddress", AddressField("0xd21cfcf820f27c42")),
                            CompositeAttribute(
                                "description",
                                StringField("We are complex individuals that have to often pull from our strengths and weaknesses in order to transcend. 3500x 3500 pixels, rendered at 350 ppi")
                            ),
                            CompositeAttribute("type", StringField("png")),
                            CompositeAttribute("edition", UInt64NumberField("1")),
                            CompositeAttribute("maxEdition", UInt64NumberField("1")),
                        )
                    )
                )
            )
        ),
    )

    private val versusDeposit = FlowLogEvent(
        type = FlowLogType.DEPOSIT,
        log = FlowLog(
            transactionHash = "610f7e5677d6a97174a98fd01e6d91e81a3ad95bf12da7f66d820e116fd96ec3",
            eventIndex = 2,
            eventType = "A.d796ff17107bbff6.Art.Deposit",
            timestamp = ZonedDateTime.parse("2021-04-30T17:42:22.077Z").toInstant(),
            blockHeight = 14013459,
            blockHash = "598ee6b7422d24662cd435106d6808022b2f20c0d8e65b26b5289afc6b9d51cb"
        ),
        event = EventMessage(
            eventId = EventId(
                type = "A",
                contractAddress = FlowAddress("0xd796ff17107bbff6"),
                contractName = "Art",
                eventName = "Deposit"
            ),
            fields = mapOf(
                "id" to UInt64NumberField("16"),
                "to" to OptionalField(AddressField("0xd796ff17107bbff6")),
            ),
        )
    )

    @Test
    fun `mint without deposit`() = runBlocking<Unit> {
        activityMaker.activities(listOf(versusMint)) should { log ->
            log.size shouldBe 1
            log.entries.first().shouldNotBeInstanceOf<MintActivity>()
        }
    }

    @Test
    fun `deposit without withdraw as transfer`() = runBlocking<Unit> {
        activityMaker.activities(listOf(versusDeposit)) should { log ->
            log.size shouldBe 1
            log.entries.first().shouldNotBeInstanceOf<TransferActivity>()
        }
    }
}
