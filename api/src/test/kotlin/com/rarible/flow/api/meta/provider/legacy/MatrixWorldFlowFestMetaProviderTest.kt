package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

internal class MatrixWorldFlowFestMetaProviderTest : FunSpec({

    val mockScript = mockk<MatrixWorldFlowFestMetaScript> {
        coEvery { call(eq(FlowAddress("0x01")), 42) } returns MatrixWorldFlowFestNftMeta(
            "i do exist",
            "existing NFT",
            "animation_url",
            "legendary"
        )

        coEvery { call(eq(FlowAddress("0x02")), any()) } returns null
    }

    val provider = MatrixWorldFlowFestMetaProvider(mockScript)

    test("should support given item") {
        provider.isSupported(
            ItemId("A.1234.MatrixWorldFlowFestNFT", 1234)
        ) shouldBe true
    }

    test("should not support given item") {
        provider.isSupported(
            ItemId("A.1234.MotoGP", 1234)
        ) shouldBe false
    }

    test("should return metadata") {
        val meta = provider.getMeta(
            mockk {
                every { id } returns ItemId("A.1234.MatrixWorldFlowFestNFT", 42)
                every { tokenId } returns 42
                every { owner } returns FlowAddress("0x01")
            }
        )
        meta as ItemMeta
        meta.name shouldBe "i do exist"
    }

    test("should return null metadata") {
        provider.getMeta(
            mockk {
                every { id } returns ItemId("A.1234.MatrixWorldFlowFestNFT", 42)
                every { tokenId } returns 42
                every { owner } returns FlowAddress("0x02")
            }
        ) shouldBe null
    }
})
