package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.OptionalField
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class KicksMetaConverterTest: FunSpec({

    test("should convert kicks meta") {
        Flow.unmarshall(
            KicksMeta::class,
            (Flow.decodeJsonCadence(JSON) as OptionalField).value!!
        ) shouldBe META
    }

}) {

    companion object {
        const val JSON = """
            {"type":"Optional","value":{"type":"Struct","value":{"id":"s.9f5ce5c01ac4c376c397fe50cea683bccf5c629e464a91c8f851e2cc7e0ac95a.Meta","fields":[{"name":"title","value":{"type":"String","value":"Union AJ4 x Dunk Mashup - Standard Edition"}},{"name":"description","value":{"type":"String","value":"Union AJ4 x Dunk Mashup - Standard Edition #17 (of 95) in the Union Jordan 4 'Off Noir' x Nike Dunk collection"}},{"name":"metadata","value":{"type":"Dictionary","value":[{"key":{"type":"String","value":"video"},"value":{"type":"String","value":"https://www.nftlx.io/videos/NFTLX_X_CLOSEDSRC_NFT.mp4"}},{"key":{"type":"String","value":"image"},"value":{"type":"String","value":"https://www.nftlx.io/images/shoes/UJ4-DNX-SV-LR.jpg"}},{"key":{"type":"String","value":"size"},"value":{"type":"String","value":"9"}},{"key":{"type":"String","value":"mediaTypes"},"value":{"type":"Array","value":[{"type":"String","value":"image"},{"type":"String","value":"video"}]}},{"key":{"type":"String","value":"media"},"value":{"type":"Dictionary","value":[{"key":{"type":"String","value":"image"},"value":{"type":"Array","value":[{"type":"String","value":"https://www.nftlx.io/images/shoes/UJ4-DNX-SV-LR.jpg"},{"type":"String","value":"https://www.nftlx.io/images/shoes/UJ4-DNX-SV-RL.jpg"}]}},{"key":{"type":"String","value":"video"},"value":{"type":"Array","value":[{"type":"String","value":"https://www.nftlx.io/videos/NFTLX_X_CLOSEDSRC_NFT.mp4"}]}}]}},{"key":{"type":"String","value":"taggedTopShot"},"value":{"type":"Optional","value":null}},{"key":{"type":"String","value":"redeemed"},"value":{"type":"Bool","value":false}}]}}]}}}
        """

        val META = KicksMeta(
            title = "Union AJ4 x Dunk Mashup - Standard Edition",
            description = "Union AJ4 x Dunk Mashup - Standard Edition #17 (of 95) in the Union Jordan 4 'Off Noir' x Nike Dunk collection",
            video = "https://www.nftlx.io/videos/NFTLX_X_CLOSEDSRC_NFT.mp4",
            image = "https://www.nftlx.io/images/shoes/UJ4-DNX-SV-LR.jpg",
            size = "9",
            redeemed = false,
            taggedTopShot = null
        )
    }
}