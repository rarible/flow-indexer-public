package com.rarible.flow.api.controller

import com.rarible.flow.api.service.NftItemService
import com.rarible.flow.core.repository.NftItemContinuation
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOrderItemControllerApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
class NftOrderItemController(
    private val nftItemService: NftItemService
): FlowNftOrderItemControllerApi {

    override suspend fun getNftOrderAllItems(continuation: String?, size: Int?, showDeleted: Boolean?): ResponseEntity<FlowNftItemsDto> {
        val cont = NftItemContinuation.parse(continuation)
        return ResponseEntity.ok(nftItemService.getAllItems(cont, size, showDeleted ?: false, null, null))
    }

    override suspend fun getNftOrderItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> = ResponseEntity.ok(nftItemService.byCollection(collection, continuation, size))

    override suspend fun getNftOrderItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> = ResponseEntity.ok(nftItemService.byAccount(owner, continuation, size))
}
