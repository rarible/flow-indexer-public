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
import com.rarible.protocol.flow.nft.api.controller.FlowNftItemControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
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

    override fun getAllItems(): ResponseEntity<Flow<FlowNftItemDto>> {
        val items: Flow<Item> = coFindAll(itemRepository)
        return ok(items)
    }

    override suspend fun getItemMeta(itemId: String): ResponseEntity<FlowItemMetaDto> {
        val itemMeta = itemMetaRepository.coFindById(ItemId.parse(itemId))
        return if (itemMeta == null) {
            ResponseEntity.status(404).build()
        } else {
            ResponseEntity.ok(ItemMetaToDtoConverter.convert(itemMeta))
        }
    }

    override fun getItemsByAccount(address: String): ResponseEntity<Flow<FlowNftItemDto>> {
        val items = itemRepository.findAllByOwner(FlowAddress(address)).asFlow()
        return ok(items)
    }

    override fun getItemsByCreator(address: String): ResponseEntity<Flow<FlowNftItemDto>> {
        val items = itemRepository.findAllByCreator(FlowAddress(address)).asFlow()
        return ok(items)
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
                itemRepository.coSave(
                    it.copy(meta = metaLink)
                )
            }

        return ResponseEntity.ok(metaLink)
    }

    private fun toDtoFlow(items: Flow<Item>): Flow<FlowNftItemDto> {
        return items.map { ItemToDtoConverter.convert(it) }
    }

}
