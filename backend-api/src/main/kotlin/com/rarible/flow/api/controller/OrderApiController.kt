package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.service.OrderService
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderIdsDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.flow.nft.api.controller.FlowOrderControllerApi
import kotlinx.coroutines.flow.Flow
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@CrossOrigin
class OrderApiController(
    private val service: OrderService
): FlowOrderControllerApi {

    override suspend fun getBidsByItem(
        contract: String,
        tokenId: String,
        status: List<FlowOrderStatusDto>,
        maker: String?,
        origin: String?,
        startDate: OffsetDateTime?,
        endDate: OffsetDateTime?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return FlowOrdersPaginationDto(emptyList()).okOr404IfNull()
    }

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

    override suspend fun getOrdersAll(
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return result(
            service.findAll(continuation, size)
        ).okOr404IfNull()
    }

    override suspend fun getOrdersAllByStatus(
        sort: String?,
        continuation: String?,
        size: Int?,
        status: List<FlowOrderStatusDto>?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return result(
            if(status == null) {
                service.findAll(continuation, size)
            } else {
                service.findAllByStatus(status, continuation, size)
            }
        ).okOr404IfNull()
    }

    override fun getOrdersByIds(flowOrderIdsDto: FlowOrderIdsDto): ResponseEntity<Flow<FlowOrderDto>> {
        return service.ordersByIds(flowOrderIdsDto.ids).okOr404IfNull()
    }

    override suspend fun getSellOrders(
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return result(
            service.findAll(continuation, size)
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
                collection, continuation, size
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
        val makerAddress = maker?.let { FlowAddress(maker) }
        val itemId = ItemId(contract, tokenId.toLong())
        return result(
            service.getSellOrdersByItemAndStatus(
                itemId, makerAddress, null, null, continuation, size
            )
        ).okOr404IfNull()
    }

    override suspend fun getSellOrdersByItemAndByStatus(
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<FlowOrderStatusDto>?,
        currencyAddress: String?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val makerAddress = maker?.let { FlowAddress(maker) }
        val currency = currencyAddress?.let { FlowAddress(currencyAddress) }
        val itemId = ItemId(contract, tokenId.toLong())
        return result(
            service.getSellOrdersByItemAndStatus(
                itemId, makerAddress, currency, status, continuation, size
            )
        ).okOr404IfNull()
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val makerAddress = FlowAddress(maker)
        val originAddress = origin?.let { FlowAddress(it) }

        return result(
            service.getSellOrdersByMaker(makerAddress, originAddress, continuation, size)
        ).okOr404IfNull()
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
