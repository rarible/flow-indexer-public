package com.rarible.flow.scanner.service

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.test.Data.createItem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

internal class ItemServiceTest: FunSpec({

    test("withItem should return null") {
        ItemService(mockk() {
            every { findById(any<ItemId>()) } returns Mono.empty()
        }).withItem(ItemId("ABC", 123L)) {
            it.contract
        } shouldBe null
    }

    test("withItem should return something") {
        val item = createItem()
        ItemService(mockk() {
            every { findById(any<ItemId>()) } returns Mono.just(item)
        }).withItem(ItemId("ABC", 123L)) {
            it.contract
        } shouldBe "ABC"
    }

})