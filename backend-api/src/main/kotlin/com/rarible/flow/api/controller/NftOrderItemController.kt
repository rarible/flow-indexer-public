package com.rarible.flow.api.controller

import com.rarible.flow.api.service.NftItemService
import com.rarible.flow.api.service.OrderItemService
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.repository.ItemFilter
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.NftItemContinuation
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOrderItemControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@CrossOrigin
@RestController
class NftOrderItemController(
    private val nftItemService: NftItemService
): FlowNftOrderItemControllerApi {

    override suspend fun getNftOrderAllItems(continuation: String?, size: Int?): ResponseEntity<FlowNftItemsDto> {
        return ResponseEntity.ok(nftItemService.getAllItems(continuation, size))
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
