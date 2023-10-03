package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.BaseIntegrationTest
import com.rarible.flow.api.IntegrationTest
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.CollectionFilter
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.repository.filters.ScrollingSort
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class CollectionServiceIt : BaseIntegrationTest() {

    @Autowired
    lateinit var collectionService: CollectionService

    @Autowired
    lateinit var itemCollectionRepository: ItemCollectionRepository

    @Test
    fun `test should read all one by one`() = runBlocking<Unit> {
        (1..9).forEach {
            itemCollectionRepository.coSave(
                ItemCollection(
                    it.toString(),
                    FlowAddress("0x0$it"),
                    "COLLECTION_$it",
                    "CLCT_$it"
                )
            )
            delay(5)
        }

        shouldReadAllByOne(
            { cont ->
                collectionService.searchAll(cont, 1).toList()
            },
            9, 0, cmp = Comparator.comparing(ItemCollection::createdDate).reversed(),
            sort = { CollectionFilter.Sort.BY_ID }
        )
    }

    suspend fun <T, S : ScrollingSort<T>> shouldReadAllByOne(
        fn: suspend (continuation: String?) -> List<T>,
        expectedCount: Int,
        currentIteration: Int = 0,
        continuation: String? = null,
        last: T? = null,
        cmp: Comparator<T>? = null,
        sort: () -> S
    ) {
        val result = fn(continuation)
        if (result.isEmpty()) {
            currentIteration shouldBe expectedCount
        } else {
            if (last != null && cmp != null) {
                listOf(last, result[0]) shouldBeSortedWith cmp
            }

            val cont = sort().nextPage(result[0])
            shouldReadAllByOne(fn, expectedCount, currentIteration + 1, cont, result[0], cmp, sort)
        }
    }
}
