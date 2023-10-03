package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

internal class CnnNFTMetaProviderTest : FunSpec({

    val cnnNftScript = mockk<CnnNftScript> {
        coEvery {
            call(FlowAddress("0xe969a6097b773709"), 2909)
        } returns CnnNFT(2909, 4, 903)
    }

    val mockMeta = mockk<CnnNFTMetaBody>("CnnNFTMetaBody") {
        every {
            toItemMeta(any())
        } returns mockk("MetaBody")
    }

    val metaScript = mockk<CnnMetaScript> {
        coEvery {
            call(4, 903)
        } returns mockMeta
    }

    test("shoud read metadata for existing item") {
        val itemId = ItemId("A.329feb3ab062d289.CNN_NFT", 2909)
        val item = mockk<Item>("item") {
            every { id } returns itemId
            every { owner } returns FlowAddress("0xe969a6097b773709")
            every { tokenId } returns 2909
        }
        mockk<ItemRepository>("itemRepository") {
            every { findById(any<ItemId>()) } returns reactor.core.publisher.Mono.just(
                item
            )
        }
        val metaProvider = CnnNFTMetaProvider(
            cnnNftScript,
            metaScript
        )

        metaProvider.getMeta(item) shouldNotBe null

        verify(exactly = 1) {
            mockMeta.toItemMeta(itemId)
        }
    }
})
