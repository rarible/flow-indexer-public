package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.data
import com.rarible.flow.api.mocks
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class CnnNftScriptTest: FunSpec({

    val scriptExecutor = mocks.scriptExecutor(
        "cnn_nft" to CNN_NFT,
        "null" to data.CADENCE_NULL
    )

    test("should return null") {
        CnnNftScript(
            scriptExecutor,
            mocks.resource("null")
        ).call(FlowAddress("0x01"), 1337) shouldBe null
    }

    test("should return cnnNft") {
        CnnNftScript(
            scriptExecutor,
            mocks.resource("cnn_nft")
        ).call(FlowAddress("0x01"), 1337) shouldBe CnnNFT(
            2909, 4, 903
        )
    }

}) {
    companion object {
        val CNN_NFT = """
            {"type":"Optional","value":{"type":"Optional","value":{"type":"Resource","value":{"id":"A.329feb3ab062d289.CNN_NFT.NFT","fields":[{"name":"uuid","value":{"type":"UInt64","value":"49237558"}},{"name":"id","value":{"type":"UInt64","value":"2909"}},{"name":"setId","value":{"type":"UInt32","value":"4"}},{"name":"editionNum","value":{"type":"UInt32","value":"903"}}]}}}}
        """.trimIndent()
    }
}