package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemFilter
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class NftItemService(
    private val itemRepository: ItemRepository
) {

    suspend fun getAllItems(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean,
        lastUpdatedFrom: Instant?,
        lastUpdatedTo: Instant?
    ): FlowNftItemsDto {
        val items = itemRepository.search(
            ItemFilter.All(showDeleted, lastUpdatedFrom, lastUpdatedTo),
            continuation,
            size
        )
        return convert(items)
    }

    suspend fun getItemById(itemId: String): FlowNftItemDto? {
        return itemRepository.coFindById(ItemId.parse(itemId))?.let {
            ItemToDtoConverter.convert(it)
        }
    }

    suspend fun byCollection(collection: String, continuation: String?, size: Int?): FlowNftItemsDto {
        val items =
            itemRepository.search(ItemFilter.ByCollection(collection), continuation, size)
        return convert(items)
    }

    private fun convert(items: List<Item>): FlowNftItemsDto =
        FlowNftItemsDto(
            continuation = nextCursor(items),
            items = items.map(ItemToDtoConverter::convert),
            total = items.size.toLong()
        )

    private suspend fun convert(items: Flow<Item>): FlowNftItemsDto {
        val result = mutableListOf<Item>()
        items.toList(result)
        return convert(result.toList())
    }

    private fun nextCursor(items: List<Item>): String? {
        return if (items.isEmpty()) {
            null
        } else {
            "${items.last().mintedAt.toEpochMilli()}_${items.last().id}"
        }
    }

    suspend fun byAccount(address: String, continuation: String?, size: Int?): FlowNftItemsDto? {
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByOwner(FlowAddress(address)), continuation, size
        )
        return convert(items)
    }

    suspend fun byCreator(address: String, continuation: String?, size: Int?): FlowNftItemsDto? {
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByCreator(FlowAddress(address)), continuation, size
        )

        return convert(items)
    }

}
