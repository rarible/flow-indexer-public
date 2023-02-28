package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.StringField
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.listener.activity.disabled.MatrixWorldVoucherActivity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainValue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

internal class MatrixWorldVoucherActivityTest: FunSpec({

    val activityMaker = MatrixWorldVoucherActivity(mockk {
        every { chainId } returns FlowChainId.MAINNET
    })

    test("should mint item") {
        activityMaker.activities(
            listOf(
                MINT_LOG_EVENT
            )
        ) shouldContainValue MintActivity(
            owner = "0x0d77ec47bbad8ef6",
            contract = "A.0d77ec47bbad8ef6.MatrixWorldVoucher",
            tokenId = 0,
            creator = "0x0d77ec47bbad8ef6",
            metadata = mapOf(
                "name" to "LandVoucher",
                "description" to "Matrix World Land Voucher",
                "animationUrl" to "",
                "hash" to "03e358fecd20e49d6f6ab8537614c2ac5de5ff5489c10e88786697aa4bc95db4",
                "type" to "Voucher"
            ),
            royalties = listOf(
                Part(FlowAddress("0x46f1e88b54fcb73c"), 0.05)
            ),
            timestamp = Instant.parse("2021-10-26T14:28:35.621Z"),
            collection = "A.0d77ec47bbad8ef6.MatrixWorldVoucher"
        )
    }

    test("tokenId") {
        activityMaker.tokenId(MINT_LOG_EVENT) shouldBe 0
    }

    test("meta") {
        activityMaker.meta(MINT_LOG_EVENT) shouldBe mapOf(
            "name" to "LandVoucher",
            "description" to "Matrix World Land Voucher",
            "animationUrl" to "",
            "hash" to "03e358fecd20e49d6f6ab8537614c2ac5de5ff5489c10e88786697aa4bc95db4",
            "type" to "Voucher"
        )
    }

    test("contractName") {
        activityMaker.contractName shouldBe "MatrixWorldVoucher"
    }

    test("isSupported") {
        activityMaker.isSupportedCollection("A.0d77ec47bbad8ef6.MatrixWorldVoucher")
    }

}) {
    companion object {
        val MINT_LOG_EVENT = FlowLogEvent(
            FlowLog(
                "aa8386f6aaaf74f7e949903c09d685e706130c6dcfd15aa5bc40d2d958efc29c",
                0,
                "A.0d77ec47bbad8ef6.MatrixWorldVoucher.Minted",
                Instant.parse("2021-10-26T14:28:35.621Z"),
                19683033,
                "85992a4b68aae43d7743cc68c5bf622655242dd841a036910230ca29fa96da49"
            ),
            event = EventMessage(
                EventId.of("A.0d77ec47bbad8ef6.MatrixWorldVoucher.Minted"),
                mapOf(
                    "id" to UInt64NumberField("0"),
                    "name" to StringField("LandVoucher"),
                    "description" to StringField("Matrix World Land Voucher"),
                    "animationUrl" to StringField(""),
                    "hash" to StringField("03e358fecd20e49d6f6ab8537614c2ac5de5ff5489c10e88786697aa4bc95db4"),
                    "type" to StringField("Voucher"),
                )
            ),
            type = FlowLogType.MINT,

        )
    }
}
