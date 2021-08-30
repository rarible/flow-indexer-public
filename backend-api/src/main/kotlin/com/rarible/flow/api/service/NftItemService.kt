package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.converter.ItemMetaToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.*
import com.rarible.protocol.dto.FlowItemMetaDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class NftItemService(
    private val itemRepository: ItemRepository,
    private val itemMetaRepository: ItemMetaRepository,
) {

    suspend fun getAllItems(continuation: String?, size: Int?, showDeleted: Boolean): Mono<FlowNftItemsDto> {
        return Mono.create { sink ->
            val cont = NftItemContinuation.parse(continuation)
            var items: Flux<Item> = if (cont != null) {
                if (showDeleted) {
                    itemRepository.findAllByDateBeforeAndIdNotOrderByDateDescIdDesc(cont.afterDate, cont.afterId)
                } else {
                    itemRepository.findAllByDateBeforeAndIdNotAndOwnerIsNotNullOrderByDateDescIdDesc(cont.afterDate, cont.afterId)
                }
            } else if (showDeleted) {
                itemRepository.findAllByOwnerIsNullOrderByDateDescIdDesc()
            } else {
                itemRepository.findAll(
                    Sort.by(
                        Sort.Order.desc(Item::date.name),
                        Sort.Order.desc(Item::tokenId.name)
                    )
                )
            }

            if (size != null) {
                items = items.take(size.toLong(), true)
            }

            items.collectList().subscribe {
                sink.success(convert(it))
            }
        }
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
        return itemMetaRepository.coFindById(ItemId.parse(itemId))?.let {
            ItemMetaToDtoConverter.convert(it)
        }
    }

    private fun convert(items: List<Item>): FlowNftItemsDto =
        FlowNftItemsDto(
            continuation = nextCursor(items),
            items = items.map(ItemToDtoConverter::convert),
            total = items.size
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
