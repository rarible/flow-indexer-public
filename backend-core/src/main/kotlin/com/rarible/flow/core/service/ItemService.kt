package com.rarible.flow.core.service

import com.mongodb.client.result.UpdateResult
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemRepository
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ItemService(
    private val itemRepository: ItemRepository
) {
    suspend fun markDeleted(itemId: ItemId): UpdateResult {
        return itemRepository.updateById(itemId, Update().set(Item::deleted.name, true))
    }

    suspend fun unlist(itemId: ItemId): UpdateResult {
        return itemRepository.updateById(itemId, Update().set(Item::listed.name, false))
    }

    suspend fun byId(itemId: ItemId): Mono<Item> = itemRepository.findById(itemId)

}
