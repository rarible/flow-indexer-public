package com.rarible.flow.api.controller

import com.rarible.flow.core.converter.ItemMetaToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.protocol.dto.FlowItemMetaDto
import com.rarible.protocol.dto.FlowItemMetaFormDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftItemControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        return ResponseEntity.ok(toDtoFlow(itemRepository.findAll()))
    }

    override suspend fun getItemMeta(itemId: String): ResponseEntity<FlowItemMetaDto> {
        val itemMeta = itemMetaRepository.findByItemId(itemId)
        if (itemMeta == null) {
            return ResponseEntity.status(404).build()
        } else {
            return ResponseEntity.ok(ItemMetaToDtoConverter.convert(itemMeta))
        }
    }

    override fun getItemsByAccount(address: String): ResponseEntity<Flow<FlowNftItemDto>> {
        return ResponseEntity.ok(toDtoFlow(itemRepository.findAllByAccount(address)))
    }

    override fun getItemsByCreator(address: String): ResponseEntity<Flow<FlowNftItemDto>> {
        return ResponseEntity.ok(toDtoFlow(itemRepository.findAllByCreator(address)))
    }

    override fun getListedItems(): ResponseEntity<Flow<FlowNftItemDto>> {
        return ResponseEntity.ok(toDtoFlow(itemRepository.findAllListed()))
    }

    override suspend fun saveItemsMeta(
        itemId: String,
        form: FlowItemMetaFormDto?
    ): ResponseEntity<String> {
        val existing = itemMetaRepository.findByItemId(itemId)
        if (existing == null) {
            itemMetaRepository.save(
                ItemMeta(itemId, form!!.title!!, form.description!!, URI.create(form.uri))
            )
        } else {
            itemMetaRepository.save(
                existing.copy(
                    title = form!!.title!!,
                    description = form.description!!,
                    uri = URI.create(form.uri)
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
