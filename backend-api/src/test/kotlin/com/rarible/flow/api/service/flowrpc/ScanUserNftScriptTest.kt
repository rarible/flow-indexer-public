package com.rarible.flow.api.service.flowrpc

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.mocks.resource
import com.rarible.flow.api.mocks.scriptExecutor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainAll

internal class ScanUserNftScriptTest: FunSpec({

    val scriptExecutor = scriptExecutor(
        "no_nfts" to """{"type":"Dictionary","value":[{"key":{"type":"String","value":"Evolution"},"value":{"type":"Array","value":[]}},{"key":{"type":"String","value":"MugenNFT"},"value":{"type":"Array","value":[]}},{"key":{"type":"String","value":"TopShot"},"value":{"type":"Array","value":[]}},{"key":{"type":"String","value":"MotoGPCard"},"value":{"type":"Array","value":[]}},{"key":{"type":"String","value":"RaribleNFT"},"value":{"type":"Array","value":[]}},{"key":{"type":"String","value":"CNN_NFT"},"value":{"type":"Array","value":[]}}]}""",
        "has_nfts" to """
            {"type":"Dictionary","value":[{"key":{"type":"String","value":"Evolution"},"value":{"type":"Array","value":[
                {"type": "UInt64", "value": 1337},
                {"type": "UInt64", "value": 1338}
            ]}}]}
        """.trimIndent()
    )

    test("should get user NFTS") {
        ScanUserNftScript(
            resource("has_nfts"),
            scriptExecutor
        ).call(FlowAddress("0x01")) shouldContainAll mapOf(
            "Evolution" to listOf(1337L, 1338L)
        )
    }

    test("should handle empty NFTs") {
        ScanUserNftScript(
            resource("no_nfts"),
            scriptExecutor
        ).call(FlowAddress("0x01")) shouldContainAll mapOf(
            "Evolution" to emptyList(),
            "MugenNFT" to emptyList(),
            "TopShot" to emptyList(),
            "MotoGPCard" to emptyList(),
            "RaribleNFT" to emptyList(),
            "CNN_NFT" to emptyList()
        )
    }

})