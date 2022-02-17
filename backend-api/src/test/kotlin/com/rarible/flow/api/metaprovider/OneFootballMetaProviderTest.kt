package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

internal class OneFootballMetaProviderTest: FunSpec({

    val notExisting = ItemId("A.6831760534292098.OneFootballCollectible", 2)
    val existing = ItemId("A.6831760534292098.OneFootballCollectible", 1)

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

    val metaScript = mockk<OneFootballMetaScript> {
        coEvery {
            call(FlowAddress("0x01"), 1)
        } returns OneFootballMetaConverterTest.META

        coEvery {
            call(FlowAddress("0x03"), 3)
        } returns null
    }

    val provider = OneFootballMetaProvider(itemRepository, metaScript)

    test("should return empty meta for non-existing item") {
        OneFootballMetaProvider(itemRepository, metaScript).getMeta(notExisting) shouldBe ItemMeta.empty(notExisting)
    }

    test("should return empty meta if script returned null") {
        OneFootballMetaProvider(
            itemRepository,
            mockk {
                coEvery { call(any(), any()) } returns null
            }
        ).getMeta(existing) shouldBe ItemMeta.empty(existing)
    }

    test("should return filled meta") {
        OneFootballMetaProvider(
            itemRepository, metaScript
        ).getMeta(existing) shouldBe OneFootballMetaConverterTest.META.toItemMeta(existing)
    }

})