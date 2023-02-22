package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

internal class JambbMomentsMetaProviderTest: FunSpec({
    val notExisting = mockk<Item> {
        every { id } returns ItemId("A.d4ad4740ee426334.Moments", 2)
        every { tokenId } returns 2

    }
    val existing = mockk<Item> {
        every { id } returns ItemId("A.d4ad4740ee426334.Moments", 1)
        every { tokenId } returns 1
        every { owner } returns FlowAddress("0x01")
    }

    val metaScript = mockk<JambbMomentsMetaScript> {
        coEvery {
            call(1)
        } returns JambbMomentsMetaConverterTest.META

        coEvery {
            call(2)
        } returns null
    }

    val properties = mockk<ApiProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }

    test("should return empty meta if script returned null") {
        JambbMomentsMetaProvider(
            metaScript,
            properties
        ).getMeta(notExisting) shouldBe null
    }

    test("should return filled meta") {
        JambbMomentsMetaProvider(
            metaScript,
            properties
        ).getMeta(existing) shouldBe JambbMomentsMetaConverterTest.META.toItemMeta(existing.id)
    }

    test("isSupported is true") {
        JambbMomentsMetaProvider(
            metaScript, properties
        ).isSupported(existing.id) shouldBe true
    }

    test("isSupported is false") {
        JambbMomentsMetaProvider(
            metaScript, properties
        ).isSupported(ItemId("A.1234.MotoGP", 1000)) shouldBe false
    }

})
