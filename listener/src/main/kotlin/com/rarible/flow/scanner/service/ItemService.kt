package com.rarible.flow.scanner.service

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import org.springframework.stereotype.Service

@Service
class ItemService(
    val itemRepository: ItemRepository,
) {

    suspend fun <T> withItem(itemId: ItemId, fn: suspend (Item) -> T): T? {
        val item = itemRepository.coFindById(itemId)
        return if (item == null) {
            null
        } else {
            fn(item)
        }
    }
}
