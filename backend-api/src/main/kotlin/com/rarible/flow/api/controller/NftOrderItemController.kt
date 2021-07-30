package com.rarible.flow.api.controller

import com.rarible.flow.api.service.OrderItemService
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOrderItemControllerApi
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
class NftOrderItemController(
    private val orderItemService: OrderItemService
): FlowNftOrderItemControllerApi {
    override suspend fun getNftOrderAllItems(continuation: String?, size: Int?): ResponseEntity<FlowNftItemsDto> =
        ResponseEntity.ok(orderItemService.allOnSale(continuation, size).awaitSingle())

    override suspend fun getNftOrderItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftItemsDto> = ResponseEntity.ok(orderItemService.getNftOrderItemsByOwner(owner, continuation, size).awaitSingle())
}
