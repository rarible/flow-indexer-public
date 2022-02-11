package com.rarible.flow.api.metaprovider

import com.rarible.flow.api.data
import com.rarible.flow.api.mocks
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

internal class JambbMomentsMetaScriptTest: FunSpec({

    val scriptExecutor = mocks.scriptExecutor(
        "null" to data.CADENCE_NULL,
        "result" to JambbMomentsMetaConverterTest.JSON
    )

    test("should hande null") {
        JambbMomentsMetaScript(
            scriptExecutor,
            mocks.resource("null")
        ).call(1337) shouldBe null
    }

    test("should handle result") {
        JambbMomentsMetaScript(
            scriptExecutor,
            mocks.resource("result")
        ).call(1337) shouldNotBe null
    }
})