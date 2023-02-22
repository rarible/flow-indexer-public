package com.rarible.flow.api.meta.provider.legacy

import com.rarible.flow.api.data
import com.rarible.flow.api.mocks
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class CnnMetaScriptTest: FunSpec({

    val goodResultKey = "ok"
    val nullResultKey = "null"

    val scriptExecutor = mocks.scriptExecutor(
        goodResultKey to METADATA,
        nullResultKey to data.CADENCE_NULL
    )

    test("should return null") {
        CnnMetaScript(
            scriptExecutor,
            mocks.resource(nullResultKey)
        ).call(4, 300) shouldBe null
    }

    test("should return hash") {
        CnnMetaScript(
            scriptExecutor,
            mocks.resource(goodResultKey)
        ).call(4, 300) shouldBe CnnNFTMetaBody(
            "Vault First Collector Coin",
            description = "A bonus collectible exclusively for Vault's earliest supporters.",
            image = "https://giglabs.mypinata.cloud/ipfs/QmcMaJNcXPPoM1jnaETeqcKRmzifP9YKU9VjpDXSHsv5bM",
            preview = "https://giglabs.mypinata.cloud/ipfs/QmSb8o7r83jLcy1eJVYSf4keRUjAz9G5MFz5yT2r5qeGcn",
            maxEditions = 1850,
            edition = 300,
            setId = 4
        )
    }

}) {
    companion object {
        val METADATA = """
            {"type":"Optional","value":{"type":"Dictionary","value":[{"key":{"type":"String","value":"description"},"value":{"type":"String","value":"A bonus collectible exclusively for Vault's earliest supporters."}},{"key":{"type":"String","value":"preview"},"value":{"type":"String","value":"https://giglabs.mypinata.cloud/ipfs/QmSb8o7r83jLcy1eJVYSf4keRUjAz9G5MFz5yT2r5qeGcn"}},{"key":{"type":"String","value":"creator_name"},"value":{"type":"String","value":"Vault by CNN"}},{"key":{"type":"String","value":"maxEditions"},"value":{"type":"String","value":"1850"}},{"key":{"type":"String","value":"ipfs_image_hash"},"value":{"type":"String","value":"QmcMaJNcXPPoM1jnaETeqcKRmzifP9YKU9VjpDXSHsv5bM"}},{"key":{"type":"String","value":"image_file_type"},"value":{"type":"String","value":"mp4"}},{"key":{"type":"String","value":"image"},"value":{"type":"String","value":"https://giglabs.mypinata.cloud/ipfs/QmcMaJNcXPPoM1jnaETeqcKRmzifP9YKU9VjpDXSHsv5bM"}},{"key":{"type":"String","value":"external_url"},"value":{"type":"String","value":"vault.cnn.com"}},{"key":{"type":"String","value":"name"},"value":{"type":"String","value":"Vault First Collector Coin"}},{"key":{"type":"String","value":"sha256_image_hash"},"value":{"type":"String","value":"ec3a3b08dc76e18522dae0699b3e960b17ed90b682b402ec62adcf41db5c9e54"}},{"key":{"type":"String","value":"additional_images"},"value":{"type":"String","value":"[]"}}]}}
        """.trimIndent()
    }
}