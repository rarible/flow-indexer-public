package com.rarible.flow.api.controller

import com.rarible.flow.api.service.OrderService
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.flow.nft.api.controller.FlowOrderControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class OrderApiController(
    private val service: OrderService
): FlowOrderControllerApi {

    override suspend fun getOrderByOrderId(orderId: String): ResponseEntity<FlowOrderDto> =
        service.orderById(orderId.toLong()).okOr404IfNull()

}
