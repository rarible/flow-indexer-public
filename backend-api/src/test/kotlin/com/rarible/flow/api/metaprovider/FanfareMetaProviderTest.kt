package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

internal class FanfareMetaProviderTest: FunSpec({

    val nonExisting = ItemId("A.4c44f3b1e4e70b20.FanfareNFTContract", 3333)
    val existing = ItemId("A.4c44f3b1e4e70b20.FanfareNFTContract", 1337)

    val provider = FanfareMetaProvider(
        mockk {
            every {
                findById(nonExisting)
            } returns Mono.empty()

            every {
                findById(existing)
            } returns Mono.just(mockk {
                every { meta } returns ObjectMapper().writeValueAsString(mapOf(
                    "metadata" to META
                ))
            })
        },
        mockk {
            every { chainId } returns FlowChainId.MAINNET
        }
    )

    test("should return empty meta for non existing item") {
        provider.getMeta(nonExisting) shouldBe ItemMeta.empty(nonExisting)
    }

    test("should return proper meta") {
        provider.getMeta(existing) shouldBe ItemMeta(
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