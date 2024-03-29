package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

internal class FanfareMetaProviderTest : FunSpec({

    val existing = ItemId("A.4c44f3b1e4e70b20.FanfareNFTContract", 1337)

    val properMeta = ItemMeta(
        itemId = existing,
        name = "ETH Denver 2022 //Fanfare",
        description = "An NFT commemorating ETH Denver 2022! Music generated from the Fanfare wallet address.",
        attributes = listOf(
            ItemMetaAttribute("genre", ""),
            ItemMetaAttribute("is_music_video", "false"),
            ItemMetaAttribute("total_copies", "1"),
            ItemMetaAttribute("edition", "1"),
        ),
        contentUrls = listOf(
            "https://nftfm-videos.s3.us-west-1.amazonaws.com/Comp_1.mp4",
            "https://fanfare-songs.s3.us-west-1.amazonaws.com/6f5fff865d4a4ce1dea4f149b3b0837e.wav",
            "https://www.fanfare.fm",
        ),
        externalUri = "https://www.fanfare.fm",
        content = listOf(
            ItemMetaContent(
                url = "https://nftfm-videos.s3.us-west-1.amazonaws.com/Comp_1.mp4",
                representation = ItemMetaContent.Representation.ORIGINAL,
                type = ItemMetaContent.Type.IMAGE,
                fileName = null,
                mimeType = null,
                size = null,
                width = null,
                height = null
            ),
            ItemMetaContent(
                url = "https://fanfare-songs.s3.us-west-1.amazonaws.com/6f5fff865d4a4ce1dea4f149b3b0837e.wav",
                representation = ItemMetaContent.Representation.ORIGINAL,
                type = ItemMetaContent.Type.AUDIO,
                fileName = null,
                mimeType = null,
                size = null,
                width = null,
                height = null
            )
        ),
    )

    val metaString =
        """{"metadata":"{\"artist_name\":\"Fanfare\",\"title\":\"ETH Denver 2022\",\"description\":\"An NFT commemorating ETH Denver 2022! Music generated from the Fanfare wallet address.\",\"genre\":\"\",\"external_url\":\"https://www.fanfare.fm\",\"image_url\":\"https://nftfm-videos.s3.us-west-1.amazonaws.com/Comp_1.mp4\",\"audio_url\":\"https://fanfare-songs.s3.us-west-1.amazonaws.com/6f5fff865d4a4ce1dea4f149b3b0837e.wav\",\"is_music_video\":false,\"total_copies\":1,\"edition\":1}"}"""

    val itemWithMeta = mockk<Item> {
        every { id } returns existing
        every { owner } returns FlowAddress("0x00")
        every { meta } returns metaString
    }

    val itemWithoutMeta = mockk<Item> {
        every { id } returns existing
        every { tokenId } returns existing.tokenId
        every { owner } returns FlowAddress("0x00")
        every { meta } returns ""
    }

    test("should return empty meta for non existing item") {
        FanfareMetaProvider(
            mockk {
                coEvery<String?> { executeFile(any<String>(), any(), any()) } returns null
            },
        ).getMeta(itemWithoutMeta) shouldBe ItemMeta.empty(existing)
    }

    test("should return proper meta from item") {
        FanfareMetaProvider(
            mocks.scriptExecutor(),
        ).getMeta(itemWithMeta) shouldBe properMeta
    }

    test("should return proper meta from flow") {
        FanfareMetaProvider(
            mockk {
                coEvery<String> { executeFile(any<String>(), any(), any()) } returns metaString
            }
        ).getMeta(itemWithMeta) shouldBe properMeta
    }
}
)
