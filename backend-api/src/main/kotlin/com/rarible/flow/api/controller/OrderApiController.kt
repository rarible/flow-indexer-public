package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.service.OrderService
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.flow.nft.api.controller.FlowOrderControllerApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class OrderApiController(
    private val service: OrderService
): FlowOrderControllerApi {
    override suspend fun getOrderBidsByItem(
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return FlowOrdersPaginationDto(emptyList()).okOr404IfNull()
    }

    override suspend fun getOrderBidsByMaker(
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return FlowOrdersPaginationDto(emptyList()).okOr404IfNull()
    }

    override suspend fun getOrderByOrderId(orderId: String): ResponseEntity<FlowOrderDto> =
        service.orderById(orderId.toLong()).okOr404IfNull()

    override suspend fun getOrdersAllByStatus(
        sort: String?,
        continuation: String?,
        size: Int?,
        status: List<FlowOrderStatusDto>?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val cont = safeContinuation(continuation)
        return result(
            if(status == null) {
                service.findAll(cont, size)
            } else {
                service.findAllByStatus(status, cont, size)
            }
        ).okOr404IfNull()
    }

    override suspend fun getSellOrders(
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val cont = safeContinuation(continuation)
        return result(
            service.findAll(cont, size)
        ).okOr404IfNull()
    }

    override suspend fun getSellOrdersByCollection(
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return result(
            service.getSellOrdersByCollection(
                collection, safeContinuation(continuation), size
            )
        ).okOr404IfNull()
    }

    override suspend fun getSellOrdersByItem(
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val makerAddress = FlowAddress(maker)
        val originAddress = origin?.let { FlowAddress(it) }
        val cont = safeContinuation(continuation)

        return result(
            service.getSellOrdersByMaker(makerAddress, originAddress, cont, size)
        ).okOr404IfNull()
    }

    private fun safeContinuation(continuation: String?): ActivityContinuation? {
        return continuation?.let { ActivityContinuation.of(it) }
    }

    private fun result(orders: List<FlowOrderDto>): FlowOrdersPaginationDto {
        return if(orders.isEmpty()) {
            FlowOrdersPaginationDto(emptyList())
        } else {
            val last = orders.last()
            FlowOrdersPaginationDto(orders, ActivityContinuation(last.createdAt, last.id.toString()).toString())
        }
    }
}
