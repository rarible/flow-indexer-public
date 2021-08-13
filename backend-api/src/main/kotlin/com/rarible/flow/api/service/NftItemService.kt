package com.rarible.flow.api.service

import com.rarible.flow.core.converter.ItemMetaToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.repository.*
import com.rarible.protocol.dto.FlowItemMetaDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class NftItemService(
    private val itemRepository: ItemRepository,
    private val itemMetaRepository: ItemMetaRepository,
    private val ownershipRepository: OwnershipRepository
) {

    suspend fun getAllItems(continuation: String?, size: Int?): FlowNftItemsDto {
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.All, NftItemContinuation.parse(continuation), size
        )
        return convert(items)
    }

    suspend fun getItemById(itemId: String): FlowNftItemDto? {
        val item = itemRepository.coFindById(ItemId.parse(itemId)) ?: return null
        return ItemToDtoConverter.convert(item)
    }

    suspend fun byCollection(collection: String, continuation: String?, size: Int?): FlowNftItemsDto {
        val items = itemRepository.search(ItemFilter.ByCollection(collection), NftItemContinuation.parse(continuation), size)
        return convert(items)
    }

    suspend fun itemMeta(itemId: String): FlowItemMetaDto? {
        val itemMeta = itemMetaRepository.coFindById(ItemId.parse(itemId)) ?: return null
        return ItemMetaToDtoConverter.convert(itemMeta)
    }

    private suspend fun convert(items: Flow<Item>): FlowNftItemsDto {
        val result = items.toList()

        return FlowNftItemsDto(
            continuation = nextCursor(result),
            items = result.map { ItemToDtoConverter.convert(it) },
            total = result.size
        )
    }

    private fun nextCursor(items: List<Item>): String? {
        return if (items.isEmpty()) {
            null
        } else {
            NftItemContinuation(items.last().date, items.last().id).toString()
        }
    }

    suspend fun byAccount(address: String, continuation: String?, size: Int?): FlowNftItemsDto? {
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByOwner(FlowAddress(address)), NftItemContinuation.parse(continuation), size
        )
        return convert(items)
    }

    suspend fun byCreator(address: String, continuation: String?, size: Int?): FlowNftItemsDto? {
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByCreator(FlowAddress(address)), NftItemContinuation.parse(continuation), size
        )

        return convert(items)
    }

}
