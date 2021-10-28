package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.service.OrderService
import com.rarible.flow.core.converter.OderStatusDtoConverter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.enum.safeOf
import com.rarible.protocol.dto.*
import com.rarible.protocol.flow.nft.api.controller.FlowOrderControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@CrossOrigin
class OrderApiController(
    private val service: OrderService
): FlowOrderControllerApi {
    override fun getBidCurrencies(itemId: String): ResponseEntity<Flow<FlowAssetDto>> =
        ResponseEntity.ok(emptyFlow())

    override suspend fun getBidsByItem(
        contract: String,
        tokenId: String,
        status: List<FlowOrderStatusDto>,
        maker: String?,
        origin: String?,
        startDate: OffsetDateTime?,
        endDate: OffsetDateTime?,
        currencyAddress: String?,
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
        currencyAddress: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return FlowOrdersPaginationDto(emptyList()).okOr404IfNull()
    }

    override suspend fun getOrderBidsByMaker(
        maker: String,
        status: List<FlowOrderStatusDto>,
        origin: String?,
        startDate: OffsetDateTime?,
        endDate: OffsetDateTime?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return FlowOrdersPaginationDto(emptyList()).okOr404IfNull()
    }

    override suspend fun getOrderByOrderId(orderId: String): ResponseEntity<FlowOrderDto> =
        result(service.orderById(orderId.toLong()))

    override suspend fun getOrdersAll(
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val sort = OrderFilter.Sort.LATEST_FIRST
        return result(
            service.findAll(continuation, size, sort),
            sort
        )
    }

    override suspend fun getOrdersAllByStatus(
        sort: String?,
        continuation: String?,
        size: Int?,
        status: List<FlowOrderStatusDto>?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val sorting = safeOf(sort, OrderFilter.Sort.LATEST_FIRST)!!
        val orderStatuses = OderStatusDtoConverter.convert(status)
        return result(
            service.findAllByStatus(orderStatuses, continuation, size, sorting),
            sorting
        )
    }

    override fun getOrdersByIds(flowOrderIdsDto: FlowOrderIdsDto): ResponseEntity<Flow<FlowOrderDto>> {
        return service.ordersByIds(flowOrderIdsDto.ids).map {
            OrderToDtoConverter.convert(it)
        }.okOr404IfNull()
    }

    override fun getSellCurrencies(itemId: String): ResponseEntity<Flow<FlowAssetDto>> {
        return service.currenciesByItemId(itemId).map {
            FlowAssetFungibleDto(it.contract, it.value)
        }.okOr404IfNull()
    }

    override suspend fun getSellOrders(
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val sort = OrderFilter.Sort.LATEST_FIRST
        return result(
            service.findAll(continuation, size, sort),
            sort
        )
    }

    override suspend fun getSellOrdersByCollection(
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val sort = OrderFilter.Sort.LATEST_FIRST
        return result(
            service.getSellOrdersByCollection(
                collection, continuation, size, sort
            ),
            sort
        )
    }

    override suspend fun getSellOrdersByItem(
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val makerAddress = maker.flowAddress()
        val itemId = ItemId(contract, tokenId.toLong())
        val sort = OrderFilter.Sort.MAKE_PRICE_ASC
        return result(
            service.getSellOrdersByItemAndStatus(
                itemId, makerAddress, null, emptyList(), continuation, size, sort
            ),
            sort
        )
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
        val makerAddress = maker.flowAddress()
        val currency = currencyAddress.flowAddress()
        val itemId = ItemId(contract, tokenId.tokenId())
        val sort = OrderFilter.Sort.MAKE_PRICE_ASC
        val orderStatuses = OderStatusDtoConverter.convert(status)
        return result(
            service.getSellOrdersByItemAndStatus(
                itemId, makerAddress, currency, orderStatuses, continuation, size, sort
            ),
            sort
        )
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val makerAddress = maker.flowAddress() as FlowAddress
        val originAddress = origin.flowAddress()
        val sort = OrderFilter.Sort.LATEST_FIRST
        return result(
            service.getSellOrdersByMaker(makerAddress, originAddress, continuation, size, sort),
            sort
        )
    }

    private fun result(order: Order?): ResponseEntity<FlowOrderDto> {
        return order?.let {
            OrderToDtoConverter.convert(it)
        }.okOr404IfNull()
    }

    private suspend fun result(orders: Flow<Order>, sort: OrderFilter.Sort): ResponseEntity<FlowOrdersPaginationDto> {
        return if(orders.count() == 0) {
            FlowOrdersPaginationDto(emptyList())
        } else {
            FlowOrdersPaginationDto(
                orders.map {
                    OrderToDtoConverter.convert(it)
                }.toList(),
                sort.nextPageSafe(orders.lastOrNull())
            )
        }.okOr404IfNull()
    }
}
