package com.rarible.flow.api.controller

import com.rarible.flow.api.service.NftItemService
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant

@CrossOrigin
@RestController
class NftApiController(
    private val nftItemService: NftItemService
) : FlowNftItemControllerApi {

    override suspend fun getNftAllItems(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ResponseEntity<FlowNftItemsDto> {
        val cont = NftItemContinuation.parse(continuation)
        return nftItemService
            .getAllItems(
                cont,
                size,
                showDeleted ?: false,
                lastUpdatedFrom?.let { Instant.ofEpochMilli(lastUpdatedFrom) },
                lastUpdatedTo?.let { Instant.ofEpochMilli(lastUpdatedTo) },
            )
            .okOr404IfNull()
    }

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

    @Deprecated("We have orders now")
    override fun getListedItems(): ResponseEntity<Flow<FlowNftItemDto>> {
        return ok(emptyFlow())
    }

    private fun ok(items: Flow<Item>) =
        ResponseEntity.ok(toDtoFlow(items))

    @Deprecated("need to remove this method")
    override suspend fun saveItemsMeta(
        itemId: String,
        flowItemMetaFormDto: FlowItemMetaFormDto?
    ): ResponseEntity<String> {
        val metaLink = "/v0.1/items/meta/$itemId"
        return ResponseEntity.ok(metaLink)
    }

    private fun toDtoFlow(items: Flow<Item>): Flow<FlowNftItemDto> {
        return items.map { ItemToDtoConverter.convert(it) }
    }

}
