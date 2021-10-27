package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.converter.ItemMetaToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.Cont
import com.rarible.flow.core.repository.ItemFilter
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.MetaDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class NftItemService(
    private val itemRepository: ItemRepository,
    private val itemMetaRepository: ItemMetaRepository
) {

    suspend fun getAllItems(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean,
        lastUpdatedFrom: Instant?,
        lastUpdatedTo: Instant?
    ): FlowNftItemsDto {
        val sort = ItemFilter.Sort.LAST_UPDATE
        val items = itemRepository.search(
            ItemFilter.All(showDeleted, lastUpdatedFrom, lastUpdatedTo),
            continuation,
            size,
            sort
        ).asFlow()
        return convert(items, sort)
    }

    suspend fun getItemById(itemId: String): FlowNftItemDto? {
        return itemRepository.coFindById(ItemId.parse(itemId))?.let {
            convertItem(it)
        }
    }

    suspend fun byCollection(collection: String, continuation: String?, size: Int?): FlowNftItemsDto {
        val sort = ItemFilter.Sort.LAST_UPDATE
        val items =
            itemRepository
                .search(ItemFilter.ByCollection(collection), continuation, size, sort)
                .asFlow()
        return convert(items, sort)
    }

    private suspend fun convert(items: Flow<Item>, sort: ItemFilter.Sort): FlowNftItemsDto {
        return if(items.count() == 0) {
            FlowNftItemsDto(0, null, emptyList())
        } else {
            FlowNftItemsDto(
                continuation = sort.nextPageSafe(items.lastOrNull()),
                items = items.map { convertItem(it) }.toList(),
                total = items.count().toLong()
            )
        }
    }

    private suspend fun convertItem(item: Item): FlowNftItemDto {
        return ItemToDtoConverter.convert(item).copy(meta = fillMeta(item.id))
    }

    private suspend fun fillMeta(id: ItemId): MetaDto? {
        val meta = itemMetaRepository.findById(id).awaitSingleOrNull() ?: return null
        return ItemMetaToDtoConverter.convert(meta)
    }

    suspend fun byAccount(address: String, continuation: String?, size: Int?): FlowNftItemsDto? {
        val sort = ItemFilter.Sort.LAST_UPDATE
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByOwner(FlowAddress(address)), continuation, size, sort
        ).asFlow()
        return convert(items, sort)
    }

    suspend fun byCreator(address: String, continuation: String?, size: Int?): FlowNftItemsDto? {
        val sort = ItemFilter.Sort.LAST_UPDATE
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByCreator(FlowAddress(address)), continuation, size, sort
        ).asFlow()

        return convert(items, sort)
    }

}
