package com.rarible.flow.api.meta.provider.legacy

import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

internal class MugenNFTMetaProviderTest : FunSpec({

    val tokenIdVal: Long = 1337
    val item = mockk<Item> {
        every { id } returns ItemId("A.1234.MugenNFT", tokenIdVal)
        every { tokenId } returns tokenIdVal
    }

    test("should return metadata") {
        val provider = MugenNFTMetaProvider(
            mocks.webClient("/1337", MUGEN_RESPONSE)
        )

        provider.getMeta(item) shouldNotBe null
    }

    test("should return null") {
        val provider = MugenNFTMetaProvider(
            WebClient.builder()
                .exchangeFunction { req ->
                    req.url().path shouldBe "/1337"

                    Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                            .header("content-type", "application/json")
                            .body("")
                            .statusCode(HttpStatus.NOT_FOUND)
                            .build()
                    )
                }.build(),
        )

        provider.getMeta(item) shouldBe null
    }

    test("should support given item") {
        MugenNFTMetaProvider(WebClient.create()).isSupported(item.id) shouldBe true
    }

    test("should not support given item") {
        MugenNFTMetaProvider(WebClient.create()).isSupported(ItemId("A.1234.MotoGP", 1234)) shouldBe false
    }

}) {

    companion object {
        val MUGEN_RESPONSE = """
            [{
                "attributes":[
                    {"trait_type":"rarity","value":"N"},
                    {"trait_type":"edition","value":1158},
                    {"trait_type":"editions","value":4000}
                ],
                "_id":"61d3d3141ac15235f75553ce",
                "address":"0x2cd46d41da4ce262",
                "type_id":9,
                "token_id":5661,
                "description":"Embedded with green sapphire, these iron badges are made to bring about luck, particularly in a sense of abundance, only warriors of courage will be awarded one of the greenish badges./nBadges of the collection are only awarded to the owners (by the time of snapshot) of MugenARt cards from Mystery Packs during Flow Fest.",
                "external_url":"https://mugenart.io",
                "icon":"https://mugen-matedata.oss-cn-beijing.aliyuncs.com/6/icon.png",
                "image":"https://mugen-matedata.oss-cn-beijing.aliyuncs.com/6/image.png",
                "image_preview":"https://mugen-matedata.oss-cn-beijing.aliyuncs.com/6/image_preview.png",
                "image_hd":"https://mugen-matedata.oss-cn-beijing.aliyuncs.com/6/image_hd.png",
                "image_blocto":"https://mugen-matedata.oss-cn-beijing.aliyuncs.com/6/image_blocto.png",
                "background_color":"ffffff",
                "animation_url":"https://mugen-matedata.oss-cn-beijing.aliyuncs.com/6/animation_url.glb",
                "animation_url_2":"https://mugen-matedata.oss-cn-beijing.aliyuncs.com/6/animation_url_2.usdz",
                "android_animation_url":"https://mugen-matedata.oss-cn-beijing.aliyuncs.com/6/android_animation_url.gltf",
                "baidu_model_key":"0",
                "youtube_url":"",
                "name":"Mugen KOD Iron"
            }]
        """.trimIndent()
    }
}