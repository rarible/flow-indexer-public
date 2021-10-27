package com.rarible.flow.api.controller

import com.rarible.flow.api.service.NftItemMetaService
import com.rarible.flow.api.service.NftItemService
import com.rarible.flow.core.converter.ItemMetaToDtoConverter
import com.rarible.flow.core.domain.ItemId
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemRoyaltyDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.MetaDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftItemControllerApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@CrossOrigin
@RestController
class NftApiController(
    private val nftItemService: NftItemService,
    private val nftItemMetaService: NftItemMetaService
) : FlowNftItemControllerApi {

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
        return ResponseEntity.ok(nftItemMetaService.getMetaByItemId(ItemId.parse(itemId)).let {
            if (it != null) ItemMetaToDtoConverter.convert(it) else null
        })
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
