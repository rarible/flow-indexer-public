package com.rarible.flow.scanner.migrations

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.repository.ItemFilter
import com.rarible.flow.core.repository.ItemRepository
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Flux


@ChangeUnit(
    id = "ChangeLog00001UpdateTopshotRoyalties",
    order = "00001",
    author = "flow"
)
class ChangeLog00001UpdateTopshotRoyalties(
    private val itemRepository: ItemRepository
) {

    @Execution
    fun changeSet() {
        runBlocking {
            updateEachBatch(update = { item ->
                item.copy(royalties = listOf(Part(royaltiesAddress, 0.05)))
            }) { continuation ->
                itemRepository.search(
                    ItemFilter.ByCollection(
                        "A.0b2a3299cc857e29.TopShot"
                    ),
                    continuation, null, ItemFilter.Sort.LAST_UPDATE
                )
            }
        }
    }

    @RollbackExecution
    fun rollback() {
        runBlocking {
            updateEachBatch(update = { item ->
                item.copy(royalties = emptyList())
            }) { continuation ->
                itemRepository.search(
                    ItemFilter.ByCollection(
                        "A.0b2a3299cc857e29.TopShot"
                    ),
                    continuation, null, ItemFilter.Sort.LAST_UPDATE
                )
            }
        }
    }

    private suspend fun updateEachBatch(
        continuation: String? = null,
        update: (Item) -> Item,
        fetch: (String?) -> Flux<Item>
    ): String? {
        val items =
            fetch(continuation).map(update).collectList().awaitFirstOrDefault(emptyList())
        return if(items.isEmpty()) {
            null
        } else {
            val last = itemRepository.saveAll(items).awaitLast()
            updateEachBatch(
                ItemFilter.Sort.LAST_UPDATE.nextPage(last),
                update,
                fetch
            )
        }
    }

    companion object {
        val royaltiesAddress = FlowAddress("0xbd69b6abdfcf4539")
    }

}
