package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.data
import com.rarible.flow.api.mocks.resource
import com.rarible.flow.api.mocks.scriptExecutor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class MatrixWorldFlowFestMetaScriptTest: FunSpec({

    val scripts = scriptExecutor(
        "no_meta" to data.CADENCE_NULL,
        "has_meta" to """
            {"type":"Optional","value":{"type":"Struct","value":{"id":"A.2d2750f240198f91.MatrixWorldFlowFestNFT.Metadata","fields":[{"name":"name","value":{"type":"String","value":"Jump Code Block"}},{"name":"description","value":{"type":"String","value":"a Jump code block for interacting with objects, 338/1500"}},{"name":"animationUrl","value":{"type":"String","value":"https://storageapi.fleek.co/124376c1-1582-4135-9fbb-f462a4f1403c-bucket/logo-07.png"}},{"name":"hash","value":{"type":"String","value":""}},{"name":"type","value":{"type":"String","value":"common"}}]}}}
        """.trimIndent()
    )

    test("should convert result to null") {
        MatrixWorldFlowFestMetaScript(
            resource("no_meta"),
            scripts
        ).call(FlowAddress("0x01"), 1) shouldBe null
    }

    test("should convert proper results") {
        MatrixWorldFlowFestMetaScript(
            resource("has_meta"),
            scripts
        ).call(FlowAddress("0x01"), 1) shouldBe MatrixWorldFlowFestNftMeta(
            name = "Jump Code Block",
            description = "a Jump code block for interacting with objects, 338/1500",
            animationUrl = "https://storageapi.fleek.co/124376c1-1582-4135-9fbb-f462a4f1403c-bucket/logo-07.png",
            type = "common"
        )
    }

})