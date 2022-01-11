package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowException
import com.rarible.flow.api.service.NftItemMetaService
import com.rarible.flow.api.service.NftItemService
import com.rarible.flow.api.service.withItemsByCollection
import com.rarible.flow.core.converter.ItemMetaToDtoConverter
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.log.Log
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemRoyaltyDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.MetaDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftItemControllerApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@CrossOrigin
@RestController
class NftApiController(
    private val nftItemService: NftItemService,
    private val nftItemMetaService: NftItemMetaService
) : FlowNftItemControllerApi {

    private val logger by Log()

    override suspend fun getNftAllItems(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ResponseEntity<FlowNftItemsDto> {
        return nftItemService
            .getAllItems(
                continuation,
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

    override suspend fun getNftItemMetaById(itemId: String): ResponseEntity<MetaDto> {
        return try {
            val meta = nftItemMetaService.getMetaByItemId(itemId.itemId())
            ItemMetaToDtoConverter.convert(meta).okOr404IfNull()
        } catch (flowEx: FlowException) {
            logger.error("Failed to get meta of [{}] from blockchain", itemId, flowEx)
            ResponseEntity.notFound().build()
        }
    }

    override suspend fun getNftItemsByOwner(
        address: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        return ResponseEntity.ok(nftItemService.byAccount(address, continuation, size))
    }

    override suspend fun resetItemMeta(itemId: String): ResponseEntity<String> {
        nftItemMetaService.resetMeta(ItemId.parse(itemId))
        return ResponseEntity.ok().build()
    }

    @PutMapping(
        value = ["/v0.1/items/refreshCollectionMeta/{collection}"],
        produces = ["application/json"]
    )
    suspend fun refreshCollectionMeta(@PathVariable("collection") collection: String): ResponseEntity<String> {
        logger.info("Refreshing metadata for collection: {}", collection)
        GlobalScope.launch {
            nftItemService.withItemsByCollection(collection, 1000) {
                nftItemMetaService.resetMeta(it.id)
                nftItemMetaService.getMetaByItemId(it.id)
            }
        }.invokeOnCompletion { error ->
            if(error == null) {
                logger.info("Successfully refreshed meta data for collection: {}", collection)
            } else {
                logger.error("Failed to refresh meta data for collection {}", collection, error)
            }
        }
        return ResponseEntity.ok().build()
    }

    override suspend fun getNftItemsByCreator(
        address: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        return ResponseEntity.ok(nftItemService.byCreator(address, continuation, size))
    }

    override suspend fun getNftItemRoyaltyById(itemId: String): ResponseEntity<FlowNftItemRoyaltyDto> {
        val itemDto = nftItemService.getItemById(itemId)

        return itemDto?.royalties?.map {
            PayInfoDto(it.account, it.value)
        }?.let {
            FlowNftItemRoyaltyDto(it)
        }.okOr404IfNull()
    }

}
