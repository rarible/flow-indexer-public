package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ItemIdTest {

    @Test
    fun `parse id with default delimiter`() {
        assertThat(
            ItemId.parse("${Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET)}:1")
        ).isEqualTo(
            ItemId(Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET), 1L)
        )
    }

    @Test
    fun `parse id with custom delimiter`() {
        assertThat(
            ItemId.parse("${Contracts.SOFT_COLLECTION.fqn(FlowChainId.TESTNET)}.1", '.')
        ).isEqualTo(
            ItemId(Contracts.SOFT_COLLECTION.fqn(FlowChainId.TESTNET), 1L)
        )
    }

    @Test
    fun `toString() with default delimiter`() {
        assertThat(
            ItemId(Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET), 1L).toString()
        ).isEqualTo(
            "${Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET)}:1"
        )
    }

    @Test
    fun `toString() with custom delimiter`() {
        assertThat(
            ItemId(Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET), 1L, '-').toString()
        ).isEqualTo(
            "${Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET)}-1"
        )
    }

    @Test
    fun `ignore hex prefix`() {
        listOf(
            "A.80102bce1de42dc4.HWGarageCard:155",
            "A.0x80102bce1de42dc4.HWGarageCard:155",
        ).onEach {
            assertThat(ItemId.parse(it).toString()).isEqualTo("A.80102bce1de42dc4.HWGarageCard:155")
        }
    }
}
