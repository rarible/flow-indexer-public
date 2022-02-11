package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

internal class JambbMomentsMetaProviderTest: FunSpec({
    val notExisting = ItemId("A.d4ad4740ee426334.Moments", 2)
    val existing = ItemId("A.d4ad4740ee426334.Moments", 1)

    val itemRepository = mockk<ItemRepository> {
        every {
            findById(eq(existing))
        } returns Mono.just(mockk {
            every { tokenId } returns 1
            every { owner } returns FlowAddress("0x01")
        })

        every {
            findById(eq(notExisting))
        } returns Mono.empty()
    }

    val metaScript = mockk<JambbMomentsMetaScript> {
        coEvery {
            call(1)
        } returns JambbMomentsMetaConverterTest.META

        coEvery {
            call(3)
        } returns null
    }

    val properties = mockk<ApiProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }

    test("should return empty meta for non-existing item") {
        JambbMomentsMetaProvider(
            itemRepository, metaScript, properties
        ).getMeta(notExisting) shouldBe ItemMeta.empty(notExisting)
    }

    test("should return empty meta if script returned null") {
        JambbMomentsMetaProvider(
            itemRepository,
            mockk {
                coEvery { call(any()) } returns null
            },
            properties
        ).getMeta(existing) shouldBe ItemMeta.empty(existing)
    }

    test("should return filled meta") {
        JambbMomentsMetaProvider(
            itemRepository,
            metaScript,
            properties
        ).getMeta(existing) shouldBe JambbMomentsMetaConverterTest.META.toItemMeta(existing)
    }

    test("isSupported is true") {
        JambbMomentsMetaProvider(
            itemRepository, metaScript, properties
        ).isSupported(existing) shouldBe true
    }

    test("isSupported is false") {
        JambbMomentsMetaProvider(
            itemRepository, metaScript, properties
        ).isSupported(ItemId("A.1234.MotoGP", 1000)) shouldBe false
    }

})