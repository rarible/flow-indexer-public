package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.data
import com.rarible.flow.api.mocks
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

internal class OneFootballMetaScriptTest: FunSpec({

    val scriptExecutor = mocks.scriptExecutor(
        "null" to data.CADENCE_NULL,
        "result" to "{\"type\":\"Optional\",\"value\":${OneFootballMetaConverterTest.JSON}}"
    )

    test("should handle null") {
        OneFootballMetaScript(
            scriptExecutor,
            mocks.resource("null")
        ).call(FlowAddress("0x01"), 1000) shouldBe null
    }

    test("should handle proper response") {
        OneFootballMetaScript(
            scriptExecutor,
            mocks.resource("result")
        ).call(FlowAddress("0x01"), 1000) shouldNotBe null
    }

})