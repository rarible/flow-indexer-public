package com.rarible.flow.api.metaprovider

import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

internal class RaribleNFTMetaProviderTest: FunSpec({

    val provider = RaribleNFTMetaProvider(
        mocks.webClient(
            "/QmR9VnAcSKLxmr3zHYaFpHAQdz13G25pvz8eWoLGHs3oAi",
            """{"name":"flying toffee","description":"my puppy <3 ","image":"ipfs://ipfs/QmbV7WN7EmhP83nK4hH2K9oitxEvjBLYRpa2NuRb86ubZN/image.jpeg","attributes":[]}"""
        )
    )

    val item = mockk<Item> {
        every { id } returns ItemId("A.1234.RaribleNFT", 1337)
        every { meta } returns """{"metaURI":"ipfs://ipfs/QmR9VnAcSKLxmr3zHYaFpHAQdz13G25pvz8eWoLGHs3oAi"}"""
    }

    test("should support RaribleNFT") {
        provider.isSupported(item.id) shouldBe true
    }

    test("should not support other NFT") {
        provider.isSupported(ItemId("A.1234.MotoGP", 42)) shouldBe false
    }

    test("should read RaribleNFT meta data").config(enabled = false) {
        provider.getMeta(
            item
        ) should { meta ->
            meta!!
            meta.name shouldBe "flying toffee"
            meta.description shouldBe "my puppy <3 "
            meta.contentUrls shouldContain "ipfs://ipfs/QmbV7WN7EmhP83nK4hH2K9oitxEvjBLYRpa2NuRb86ubZN/image.jpeg"
        }
    }

})
