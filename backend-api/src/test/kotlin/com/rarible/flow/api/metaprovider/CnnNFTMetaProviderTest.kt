package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono


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
        val metaProvider = CnnNFTMetaProvider(
            mockk("itemRepository") {
                every { findById(any<ItemId>()) } returns Mono.just(
                    mockk("item") {
                        every { id } returns itemId
                        every { owner } returns FlowAddress("0xe969a6097b773709")
                        every { tokenId } returns 2909
                    }
                )
            },
            cnnNftScript,
            metaScript
        )

        metaProvider.getMeta(itemId) shouldNotBe null

        verify(exactly = 1) {
            mockMeta.toItemMeta(itemId)
        }
    }


})
