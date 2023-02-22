package com.rarible.flow.api.controller

import com.rarible.flow.api.meta.ItemMetaConverter
import com.rarible.flow.api.meta.MetaException
import com.rarible.flow.api.service.ItemRoyaltyService
import com.rarible.flow.api.service.NftItemMetaService
import com.rarible.flow.api.service.NftItemService
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.util.Log
import com.rarible.protocol.dto.FlowItemIdsDto
import com.rarible.protocol.dto.FlowMetaDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemRoyaltyDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftItemControllerApi
import kotlinx.coroutines.DelicateCoroutinesApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@DelicateCoroutinesApi
@CrossOrigin
@RestController
class NftApiController(
    private val nftItemService: NftItemService,
    private val nftItemMetaService: NftItemMetaService,
    private val itemRoyaltyService: ItemRoyaltyService,
) : FlowNftItemControllerApi {

    private val logger by Log()

    override suspend fun getItemByIds(flowItemIdsDto: FlowItemIdsDto): ResponseEntity<FlowNftItemsDto> {
        val result = nftItemService.getItemsByIds(flowItemIdsDto.ids.map { ItemId.parse(it) })
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftAllItems(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
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
        return nftItemService.getItemById(itemId.itemId()).okOr404IfNull()
    }

    override suspend fun getNftItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        return ResponseEntity.ok(nftItemService.byCollection(collection, continuation, size))
    }

    override suspend fun getNftItemMetaById(itemId: String): ResponseEntity<FlowMetaDto> {
        val id = itemId.itemId()
        val meta = try {
            val meta = nftItemMetaService.getMetaByItemId(id)
            meta?.let { ItemMetaConverter.convert(meta) }
        } catch (e: MetaException) {
            logger.error("Failed to get meta of [{}] from blockchain: {}", itemId, e.message)
            withStatus(e.status.toDto())
        } catch (e: Exception) {
            logger.error("Failed to get meta of [{}] from blockchain: ", itemId, e)
            withStatus(FlowMetaDto.Status.ERROR)
        }
        val result = meta ?: withStatus(FlowMetaDto.Status.NOT_FOUND)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftItemsByOwner(
        address: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> {
        return ResponseEntity.ok(nftItemService.byAccount(address, continuation, size))
    }

    override suspend fun resetItemMeta(itemId: String): ResponseEntity<String> {
        return ResponseEntity.ok().build()
    }

    override suspend fun getNftItemsByCreator(
        address: String,
        continuation: String?,
        size: Int?,
    ): ResponseEntity<FlowNftItemsDto> {
        return ResponseEntity.ok(nftItemService.byCreator(address, continuation, size))
    }

    override suspend fun getNftItemRoyaltyById(itemId: String): ResponseEntity<FlowNftItemRoyaltyDto> {
        val royalty = itemRoyaltyService
            .getRoyaltiesByItemId(itemId.itemId())
            ?.map { PayInfoDto(it.address, it.fee) } ?: emptyList()
        return FlowNftItemRoyaltyDto(royalty).okOr404IfNull()
    }

    private fun withStatus(status: FlowMetaDto.Status): FlowMetaDto {
        return FlowMetaDto(name = "", status = status)
    }
}
