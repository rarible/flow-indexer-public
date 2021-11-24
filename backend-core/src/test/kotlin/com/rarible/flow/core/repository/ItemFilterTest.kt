package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*
import java.time.Instant

internal class ItemFilterTest: FunSpec({

    val now = Instant.now()

    test("ByShowDeleted") {
        ItemFilter.ByShowDeleted(true).criteria() shouldBe Criteria()

        ItemFilter.ByShowDeleted(false).criteria() shouldBe Criteria().andOperator(
            Item::owner exists true,
            Item::owner ne null
        )
    }

    test("ByLastUpdatedTo") {
        ItemFilter.ByLastUpdatedTo(null).criteria() shouldBe Criteria()

        ItemFilter.ByLastUpdatedTo(now).criteria() shouldBe (
            Item::updatedAt lt now
        )
    }

    test("ByLastUpdatedFrom") {
        ItemFilter.ByLastUpdatedFrom(null).criteria() shouldBe Criteria()

        ItemFilter.ByLastUpdatedFrom(now).criteria() shouldBe (
            Item::updatedAt gte now
        )
    }

    test("item filter - sort ") {
        val sort = ItemFilter.Sort.LAST_UPDATE
        sort.springSort() shouldBe Sort.by(
            Sort.Order.desc(Item::updatedAt.name),
            Sort.Order.desc(Item::id.name)
        )

        val entities = flowOf<Item>(
            mockk(), mockk() {
                every { updatedAt } returns now
                every { id } returns ItemId("ABC", 1000)
            }
        )
        sort.nextPage(entities, 3) shouldBe null
        sort.nextPage(entities, 2) shouldBe "${now.toEpochMilli()}_ABC:1000"
        sort.nextPageSafe(null) shouldBe null
    }




})
