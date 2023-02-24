package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.api.royalty.provider.ItemRoyaltyProvider
import com.rarible.flow.api.royalty.provider.Royalty
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.repository.ItemFilter
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.repository.withEntity
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ItemService(
    private val itemRepository: ItemRepository,
    private val royaltyProviders: List<ItemRoyaltyProvider>,
    private val metaProviders: List<ItemMetaProvider>,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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
        return convert(items, sort, size)
    }

    suspend fun getItemById(itemId: ItemId): FlowNftItemDto? {
        return itemRepository.coFindById(itemId)?.let {
            ItemToDtoConverter.convert(it)
        }
    }

    suspend fun getMetaByItemId(itemId: ItemId): ItemMeta? {
        val item = itemRepository.coFindById(itemId)

        if (item == null) {
            logger.warn("Unable to fetch meta for items that doesn't exists: $itemId")
            return null
        }

        val provider = metaProviders.firstOrNull { it.isSupported(itemId) }
            ?: throw IllegalArgumentException("No meta provider found for item $itemId")

        return provider.getMeta(item)
    }

    suspend fun byCollectionRaw(collection: String, continuation: String?, size: Int?): Flow<Item> {
        val sort = ItemFilter.Sort.LAST_UPDATE
        return itemRepository
            .search(ItemFilter.ByCollection(collection), continuation, size, sort)
            .asFlow()
    }

    suspend fun byCollection(collection: String, continuation: String?, size: Int?): FlowNftItemsDto {
        val sort = ItemFilter.Sort.LAST_UPDATE
        val items = byCollectionRaw(collection, continuation, size)
        return convert(items, sort, size)
    }

    private suspend fun convert(items: Flow<Item>, sort: ItemFilter.Sort, size: Int?): FlowNftItemsDto {
        return if (items.count() == 0) {
            FlowNftItemsDto(null, emptyList())
        } else {
            FlowNftItemsDto(
                continuation = sort.nextPage(items, size),
                items = items.map { ItemToDtoConverter.convert(it) }.toList()
            )
        }
    }

    suspend fun byAccount(address: String, continuation: String?, size: Int?): FlowNftItemsDto? {
        val sort = ItemFilter.Sort.LAST_UPDATE
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByOwner(FlowAddress(address)), continuation, size, sort
        ).asFlow()
        return convert(items, sort, size)
    }

    suspend fun byCreator(address: String, continuation: String?, size: Int?): FlowNftItemsDto? {
        val sort = ItemFilter.Sort.LAST_UPDATE
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByCreator(FlowAddress(address)), continuation, size, sort
        ).asFlow()

        return convert(items, sort, size)
    }

    suspend fun getItemsByIds(ids: List<ItemId>): FlowNftItemsDto {
        val items = itemRepository.findAllByIdIn(ids.toSet()).asFlow().toList()
        return if (items.isEmpty()) {
            FlowNftItemsDto(null, emptyList())
        } else {
            FlowNftItemsDto(
                continuation = null,
                items = items.map { ItemToDtoConverter.convert(it) }.toList()
            )
        }
    }

    suspend fun getItemRoyaltiesById(itemId: ItemId): List<Royalty>? {
        return itemRepository.withEntity(itemId) { item ->
            item.royalties
                .map { Royalty(it.address.formatted, it.fee.toBigDecimal()) }
                .ifEmpty { getRoyalties(itemId)?.also { saveRoyalties(item, it) } }
        }
    }

    private suspend fun getRoyalties(itemId: ItemId): List<Royalty>? =
        royaltyProviders.find { it.isSupported(itemId) }?.let { provider ->
            itemRepository.withEntity(itemId, provider::getRoyalties)
        }

    private suspend fun saveRoyalties(item: Item, royalties: List<Royalty>): Item {
        val toSave = royalties.map { Part(FlowAddress(it.address), it.fee.toDouble()) }
        return itemRepository.coSave(item.copy(royalties = toSave))
    }

}