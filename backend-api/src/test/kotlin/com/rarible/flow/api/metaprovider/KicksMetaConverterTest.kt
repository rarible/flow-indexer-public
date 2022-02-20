package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class KicksMetaConverterTest: FunSpec({

    test("should convert kicks meta") {
        Flow.unmarshall(
            KicksMeta::class,
            Flow.decodeJsonCadence(JSON)
        ) shouldBe META
    }

}) {

    companion object {
        const val JSON = """
            {"type":"Optional","value":{"type":"Struct","value":{"id":"s.938018da33b0ffaf386846eb9b22da313d6608fa02c784c5bdd2bf38c49c8daf.Meta","fields":[{"name":"title","value":{"type":"String","value":"Union AJ4 x Dunk Mashup - Standard Edition"}},{"name":"description","value":{"type":"String","value":"Union AJ4 x Dunk Mashup - Standard Edition #17 (of 95) in the Union Jordan 4 'Off Noir' x Nike Dunk collection"}},{"name":"media","value":{"type":"Array","value":[]}},{"name":"redeemed","value":{"type":"Bool","value":false}},{"name":"taggedTopShot","value":{"type":"Optional","value":null}},{"name":"metadata","value":{"type":"Dictionary","value":[{"key":{"type":"String","value":"video"},"value":{"type":"String","value":"https://www.nftlx.io/videos/NFTLX_X_CLOSEDSRC_NFT.mp4"}},{"key":{"type":"String","value":"image"},"value":{"type":"String","value":"https://www.nftlx.io/images/shoes/UJ4-DNX-SV-LR.jpg"}},{"key":{"type":"String","value":"size"},"value":{"type":"String","value":"9"}},{"key":{"type":"String","value":"mediaTypes"},"value":{"type":"Array","value":[{"type":"String","value":"image"},{"type":"String","value":"video"}]}},{"key":{"type":"String","value":"media"},"value":{"type":"Dictionary","value":[{"key":{"type":"String","value":"image"},"value":{"type":"Array","value":[{"type":"String","value":"https://www.nftlx.io/images/shoes/UJ4-DNX-SV-LR.jpg"},{"type":"String","value":"https://www.nftlx.io/images/shoes/UJ4-DNX-SV-RL.jpg"}]}},{"key":{"type":"String","value":"video"},"value":{"type":"Array","value":[{"type":"String","value":"https://www.nftlx.io/videos/NFTLX_X_CLOSEDSRC_NFT.mp4"}]}}]}},{"key":{"type":"String","value":"taggedTopShot"},"value":{"type":"Optional","value":null}},{"key":{"type":"String","value":"redeemed"},"value":{"type":"Bool","value":false}}]}}]}}}
        """

        val META = KicksMeta(
            title = "Union AJ4 x Dunk Mashup - Standard Edition",
            description = "Union AJ4 x Dunk Mashup - Standard Edition #17 (of 95) in the Union Jordan 4 'Off Noir' x Nike Dunk collection",

        )
    }
}