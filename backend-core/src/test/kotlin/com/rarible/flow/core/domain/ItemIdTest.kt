package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class ItemIdTest: FunSpec({
    test("parse id with default delimiter") {
        ItemId.parse("${Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET)}:1") shouldBe ItemId(
            Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET), 1L
        )
    }

    test("parse id with custom delimiter") {
        ItemId.parse("${Contracts.SOFT_COLLECTION.fqn(FlowChainId.TESTNET)}.1", '.') shouldBe ItemId(
            Contracts.SOFT_COLLECTION.fqn(FlowChainId.TESTNET), 1L
        )
    }

    test("toString() with default delimiter") {
        ItemId(Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET), 1L).toString() shouldBe "${Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET)}:1"
    }

    test("toString() with custom delimiter") {
        ItemId(Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET), 1L, '-').toString() shouldBe "${Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET)}-1"
    }
})
