package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

internal class OneFootballMetaProviderTest: FunSpec({

    val notExisting = mockk<Item> {
        every { id } returns ItemId("A.6831760534292098.OneFootballCollectible", 3)
        every { tokenId } returns 3
        every { owner } returns FlowAddress("0x03")
    }
    val existing = mockk<Item> {
        every { id } returns ItemId("A.6831760534292098.OneFootballCollectible", 1)
        every { tokenId } returns 1
        every { owner } returns FlowAddress("0x01")
    }

    val itemRepository = mockk<ItemRepository> {
        every {
            findById(eq(existing.id))
        } returns Mono.just(existing)

        every {
            findById(eq(notExisting.id))
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

    test("should return empty meta if script returned null") {
        provider.getMeta(notExisting) shouldBe null
    }

    test("should return filled meta") {
        provider.getMeta(existing) shouldBe OneFootballMetaConverterTest.META.toItemMeta(existing.id)
    }

})
