package com.rarible.flow.core.repository.filters

import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe

class ScrollingSortDefaultsTest : FunSpec({

    test("should provide page size") {
        listOf(
            -1 to 50,
            null to 50,
            42 to 42,
            51 to 51,
            1100 to 1000,
        ).forAll { (input: Int?, output: Int) ->
            ScrollingSort.pageSize(input) shouldBe output
        }
    }
})
