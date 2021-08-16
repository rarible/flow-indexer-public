package com.rarible.flow.api.controller

import com.rarible.flow.api.service.NftItemService
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.protocol.dto.FlowItemMetaDto
import com.rarible.protocol.dto.FlowItemMetaFormDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftItemControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@CrossOrigin
@RestController
class NftApiController(
    private val itemRepository: ItemRepository,
    private val itemMetaRepository: ItemMetaRepository,
    private val nftItemService: NftItemService
) : FlowNftItemControllerApi {

    override suspend fun getNftAllItems(continuation: String?, size: Int?, showDeleted: Boolean?): ResponseEntity<FlowNftItemsDto> =
        ResponseEntity.ok(nftItemService.getAllItems(continuation, size, showDeleted ?: false))

    override suspend fun getNftItemById(itemId: String): ResponseEntity<FlowNftItemDto> {
        return nftItemService.getItemById(itemId).okOr404IfNull()
    }

    override suspend fun getNftItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        return ResponseEntity.ok(nftItemService.byCollection(collection, continuation, size))
    }

    override suspend fun getNftItemMetaById(itemId: String): ResponseEntity<FlowItemMetaDto> {
        return nftItemService.itemMeta(itemId).okOr404IfNull()
    }

    override suspend fun getNftItemsByOwner(
        address: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        return ResponseEntity.ok(nftItemService.byAccount(address, continuation, size))
    }

    override suspend fun getNftItemsByCreator(
        address: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        return ResponseEntity.ok(nftItemService.byCreator(address, continuation, size))
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
        itemMetaRepository.coSave(
            existing?.copy(
                title = flowItemMetaFormDto!!.title!!,
                description = flowItemMetaFormDto.description!!,
                uri = URI.create(flowItemMetaFormDto.uri!!)
            )
                ?: ItemMeta(
                    id,
                    flowItemMetaFormDto!!.title!!,
                    flowItemMetaFormDto.description!!,
                    URI.create(flowItemMetaFormDto.uri!!)
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

}
