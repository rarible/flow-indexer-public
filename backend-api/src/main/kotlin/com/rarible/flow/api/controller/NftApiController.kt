package com.rarible.flow.api.controller

import com.rarible.flow.core.converter.ItemMetaToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.*
import com.rarible.protocol.dto.FlowItemMetaDto
import com.rarible.protocol.dto.FlowItemMetaFormDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftItemControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.onflow.sdk.FlowAddress
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@CrossOrigin
@RestController
class NftApiController(
    private val itemRepository: ItemRepository,
    private val itemMetaRepository: ItemMetaRepository
) : FlowNftItemControllerApi {

    override suspend fun getAllItems(continuation: String?, size: Int?): ResponseEntity<FlowNftItemsDto> {
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.All, NftItemContinuation.parse(continuation), size
        )

        return ResponseEntity.ok(convert(items))
    }

    override suspend fun getNftItemById(itemId: String): ResponseEntity<FlowNftItemDto> {
        val item = itemRepository.coFindById(ItemId.parse(itemId))
        return if (item == null) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(
                ItemToDtoConverter.convert(item)
            )
        }
    }

    override suspend fun getNftItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        val items = itemRepository.search(ItemFilter.ByCollection(collection), NftItemContinuation.parse(continuation), size)
        return ResponseEntity.ok(convert(items))
    }

    override suspend fun getItemMeta(itemId: String): ResponseEntity<FlowItemMetaDto> {
        val itemMeta = itemMetaRepository.coFindById(ItemId.parse(itemId))
        return if (itemMeta == null) {
            ResponseEntity.status(404).build()
        } else {
            ResponseEntity.ok(ItemMetaToDtoConverter.convert(itemMeta))
        }
    }

    override suspend fun getItemsByAccount(
        address: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByOwner(FlowAddress(address)), NftItemContinuation.parse(continuation), size
        )

        return ResponseEntity.ok(convert(items))
    }

    override suspend fun getItemsByCreator(
        address: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        val items: Flow<Item> = itemRepository.search(
            ItemFilter.ByCreator(FlowAddress(address)), NftItemContinuation.parse(continuation), size
        )

        return ResponseEntity.ok(convert(items))
    }

    override fun getListedItems(): ResponseEntity<Flow<FlowNftItemDto>> {
        val items = itemRepository.findAllByListedIsTrue().asFlow()
        return ok(items)
    }

    private fun ok(items: Flow<Item>) =
        ResponseEntity.ok(toDtoFlow(items))

    override suspend fun saveItemsMeta(
        itemId: String,
        flowItemMetaFormDto: FlowItemMetaFormDto?
    ): ResponseEntity<String> {
        val id = ItemId.parse(itemId)
        val existing: ItemMeta? = itemMetaRepository.findById(id).awaitSingleOrNull()
        itemMetaRepository.save(
            existing?.copy(
                title = flowItemMetaFormDto!!.title!!,
                description = flowItemMetaFormDto.description!!,
                uri = URI.create(flowItemMetaFormDto.uri)
            )
                ?: ItemMeta(
                    id,
                    flowItemMetaFormDto!!.title!!,
                    flowItemMetaFormDto.description!!,
                    URI.create(flowItemMetaFormDto.uri)
                )
        )

        val metaLink = "/v0.1/items/meta/$itemId"
        itemRepository.coFindById(id)
            ?.let {
                itemRepository.save(
                    it.copy(meta = metaLink)
                )
            }

        return ResponseEntity.ok(metaLink)
    }

    private fun toDtoFlow(items: Flow<Item>): Flow<FlowNftItemDto> {
        return items.map { ItemToDtoConverter.convert(it) }
    }

    private suspend fun convert(items: Flow<Item>): FlowNftItemsDto {
        val result = items.toList()

        return FlowNftItemsDto(
            nextCursor(result),
            result.map { ItemToDtoConverter.convert(it) }
        )
    }

    private fun nextCursor(items: List<Item>): String? {
        return if (items.isEmpty()) {
            null
        } else {
            NftItemContinuation(items.last().date, items.last().id).toString()
        }
    }

}
