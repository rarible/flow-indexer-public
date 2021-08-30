package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class OwnershipIdTest: FunSpec({
    test("parse id") {
        OwnershipId.parse("0x01:1:${FlowAddress("0x02").formatted}") shouldBe OwnershipId(
            contract = "0x01", tokenId = 1L, owner = FlowAddress("0x02")
        )
    }

    test("toString()") {
        OwnershipId(
            contract = "0x01", tokenId = 1L, owner = FlowAddress("0x02")
        ).toString() shouldBe "0x01:1:${FlowAddress("0x02").formatted}"
    }
})
