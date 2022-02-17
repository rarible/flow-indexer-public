package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus

internal class FanfareMetaProviderTest: FunSpec({

    val nonExisting = ItemId("A.4c44f3b1e4e70b20.FanfareNFTContract", 3333)
    val existing = ItemId("A.4c44f3b1e4e70b20.FanfareNFTContract", 1337)

    val apiProperties = mockk<ApiProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }

    test("should return empty meta for non existing item") {
        FanfareMetaProvider(
            mocks.webClient(
                "https://www.fanfare.fm/api/nft-meta/3333",
                "{}",
                HttpStatus.resolve(500)!!
            ),
            apiProperties
        ).getMeta(nonExisting) shouldBe ItemMeta.empty(nonExisting)
    }

    test("should return proper meta") {
        FanfareMetaProvider(
            mocks.webClient(
                "https://www.fanfare.fm/api/nft-meta/1337",
                META
            ),
            apiProperties
        ).getMeta(existing) shouldBe ItemMeta(
            existing,
            "Sea of Tranquility (WSOGMM version)",
            "Sea of Tranquility is an unreleased",
            listOf(
                ItemMetaAttribute("release date", "2022-01-26"),
                ItemMetaAttribute("artist", "population-of-mars"),
                ItemMetaAttribute("quantity", "42")
            ),
            listOf(
                "https://fanfare-nft-images.s3.us-west-1.amazonaws.com/0x190d7b3f05cbf6d8/watertest02.mp4",
                "https://fanfare-nft-audio.s3.us-west-1.amazonaws.com/0x190d7b3f05cbf6d8/Sea+of+Tranquility+-+PopOfMars.wav"
            )
        )
    }

}) {
    companion object {
        val META = """
            {
              "external_url": "https://www.fanfare.fm/artist/population-of-mars/38",
              "id": 38,
              "description": "Sea of Tranquility is an unreleased",
              "name": "Sea of Tranquility (WSOGMM version)",
              "attributes": [
                {
                  "display_type": "date",
                  "trait_type": "release date",
                  "value": 1643198110
                },
                {
                  "trait_type": "artist",
                  "value": "population-of-mars"
                },
                {
                  "display_type": "number",
                  "trait_type": "quantity",
                  "value": 42
                }
              ],
              "content": {
                "audio_url": "https://fanfare-nft-audio.s3.us-west-1.amazonaws.com/0x190d7b3f05cbf6d8/Sea+of+Tranquility+-+PopOfMars.wav",
                "image": "https://fanfare-nft-images.s3.us-west-1.amazonaws.com/0x190d7b3f05cbf6d8/watertest02.mp4"
              },
              "audio_url": "https://fanfare-nft-audio.s3.us-west-1.amazonaws.com/0x190d7b3f05cbf6d8/Sea+of+Tranquility+-+PopOfMars.wav",
              "image": "https://fanfare-nft-images.s3.us-west-1.amazonaws.com/0x190d7b3f05cbf6d8/watertest02.mp4"
            }
        """.trimIndent()
    }
}