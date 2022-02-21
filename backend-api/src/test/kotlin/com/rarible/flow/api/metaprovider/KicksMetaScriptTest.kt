package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.data
import com.rarible.flow.api.mocks
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class KicksMetaScriptTest: FunSpec({

    val scriptExecutor = mocks.scriptExecutor(
        "null" to data.CADENCE_NULL,
        "result" to KicksMetaConverterTest.JSON
    )

    test("should handle null") {
        KicksMetaScript(
            scriptExecutor,
            mocks.resource("null")
        ).call(FlowAddress("0x01"), 1000) shouldBe null
    }

    test("should handle proper response") {
        KicksMetaScript(
            scriptExecutor,
            mocks.resource("result")
        ).call(FlowAddress("0x01"), 1000) shouldBe KicksMetaConverterTest.META
    }

})