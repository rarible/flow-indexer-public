package com.rarible.flow.api.royaltyprovider

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal

internal class OneFootballRoyaltyProviderTest: FunSpec({

    val provider = OneFootballRoyaltyProvider(mockk {
        every { chainId } returns FlowChainId.MAINNET
    })

    test("should be supported") {
        provider.isSupported(
            ItemId("A.6831760534292098.OneFootballCollectible", 1337)
        ) shouldBe true
    }

    test("should not be supported") {
        provider.isSupported(
            ItemId("A.6831760534292098.MatrixWorldVoucher", 1337)
        ) shouldBe false
    }

    test("should return static royalties") {
        provider.getRoyalty(mockk()) shouldContainExactly listOf(
            Royalty("0x6831760534292098", BigDecimal("0.005"))
        )
    }

})