package com.rarible.flow.api.controller

import com.rarible.flow.core.converter.ItemMetaToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.protocol.dto.FlowItemMetaDto
import com.rarible.protocol.dto.FlowItemMetaFormDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftItemControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        return ok(itemRepository.findAll())
    }

    override suspend fun getItemMeta(itemId: String): ResponseEntity<FlowItemMetaDto> {
        val itemMeta = itemMetaRepository.findByItemId(itemId)
        return if (itemMeta == null) {
            ResponseEntity.status(404).build()
        } else {
            ResponseEntity.ok(ItemMetaToDtoConverter.convert(itemMeta))
        }
    }

    override fun getItemsByAccount(address: String): ResponseEntity<Flow<FlowNftItemDto>> {
        val items = itemRepository.findAllByAccount(FlowAddress(address))
        return ok(items)
    }

    override fun getItemsByCreator(address: String): ResponseEntity<Flow<FlowNftItemDto>> {
        return ok(itemRepository.findAllByCreator(FlowAddress(address)))
    }

    override fun getListedItems(): ResponseEntity<Flow<FlowNftItemDto>> {
        return ok(itemRepository.findAllListed())
    }

    private fun ok(items: Flow<Item>) =
        ResponseEntity.ok(toDtoFlow(items))

    override suspend fun saveItemsMeta(
        itemId: String,
        flowItemMetaFormDto: FlowItemMetaFormDto?
    ): ResponseEntity<String> {
        val existing = itemMetaRepository.findByItemId(itemId)
        if (existing == null) {
            itemMetaRepository.save(
                ItemMeta(ItemId.parse(itemId), flowItemMetaFormDto!!.title!!, flowItemMetaFormDto.description!!, URI.create(flowItemMetaFormDto.uri))
            )
        } else {
            itemMetaRepository.save(
                existing.copy(
                    title = flowItemMetaFormDto!!.title!!,
                    description = flowItemMetaFormDto.description!!,
                    uri = URI.create(flowItemMetaFormDto.uri)
                )
            )
        }

        val metaLink = "/v0.1/items/meta/$itemId"
        itemRepository
            .findById(itemId)
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
