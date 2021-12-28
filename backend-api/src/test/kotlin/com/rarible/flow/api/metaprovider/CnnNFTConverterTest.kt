package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowScriptResponse
import com.nftco.flow.sdk.cadence.OptionalField
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class CnnNFTConverterTest: FunSpec({

    val expected = CnnNFT(id = 31689, setId = 8, editionNum = 230)

    test("should convert response from script") {
        CnnNFTConverter.convert(
            FlowScriptResponse(
                Flow.decodeJsonCadence(SCRIPT_RESPONSE)
            ).jsonCadence as OptionalField
        ) shouldBe expected
    }

    test("should convert single optional wrap") {
        CnnNFTConverter.convert(
            FlowScriptResponse(
                Flow.decodeJsonCadence(SINGLE_OPTIONAL_WRAP)
            ).jsonCadence as OptionalField
        ) shouldBe expected
    }

    test("should convert pure resource") {
        CnnNFTConverter.convert(
            FlowScriptResponse(
                Flow.decodeJsonCadence(SINGLE_OPTIONAL_WRAP)
            ).jsonCadence as OptionalField
        ) shouldBe expected
    }

    test("should convert nil") {
        CnnNFTConverter.convert(
            FlowScriptResponse(
                Flow.decodeJsonCadence(NULL)
            ).jsonCadence as OptionalField
        ) shouldBe null
    }
}) {

    companion object {
        val RESOURCE = """
            {"type":"Resource","value":{"id":"A.329feb3ab062d289.CNN_NFT.NFT","fields":[{"name":"uuid","value":{"type":"UInt64","value":"58716060"}},{"name":"id","value":{"type":"UInt64","value":"31689"}},{"name":"setId","value":{"type":"UInt32","value":"8"}},{"name":"editionNum","value":{"type":"UInt32","value":"230"}}]}}
        """.trimIndent()

        val SINGLE_OPTIONAL_WRAP = """
            {"type":"Optional","value":$RESOURCE}
        """.trimIndent()

        val SCRIPT_RESPONSE = """
            {"type":"Optional","value":$SINGLE_OPTIONAL_WRAP}
        """.trimIndent()

        val NULL = """
            {"type":"Optional","value":null}
        """.trimIndent()
    }
}