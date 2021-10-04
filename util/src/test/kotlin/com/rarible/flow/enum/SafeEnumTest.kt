package com.rarible.flow.enum

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe


class SafeEnumTest: FunSpec({

    test("should get enum value") {
        TestEnum.values().forEach {
            safeOf<TestEnum>(it.name) shouldBe it
        }
    }

    test("should return null") {
        safeOf<TestEnum>("E") shouldBe null
    }

    test("should return default") {
        safeOf<TestEnum>("E", TestEnum.A) shouldBe TestEnum.A
    }

    test("should make list of known enums") {
        safeOf<TestEnum>("A", "B", "C", "D", "E", "X") shouldContainAll TestEnum.values().toList()
    }

    test("should make list of known enums from list") {
        safeOf<TestEnum>(listOf("A", "B", "C", "D", "E", "X")) shouldContainAll TestEnum.values().toList()
    }

    test("should make default list") {
        safeOf(listOf("E", "X"), listOf(TestEnum.A)) shouldContainAll listOf(TestEnum.A)
    }
})

enum class TestEnum {
    A, B, C, D
}