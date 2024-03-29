package com.rarible.flow.api.controller

import com.rarible.flow.api.service.OrderService
import com.rarible.flow.api.util.flowAddress
import com.rarible.flow.api.util.itemId
import com.rarible.flow.api.util.okOr404IfNull
import com.rarible.flow.api.util.tokenId
import com.rarible.flow.core.converter.OderStatusDtoConverter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.util.safeOf
import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderIdsDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.flow.nft.api.controller.FlowBidOrderControllerApi
import com.rarible.protocol.flow.nft.api.controller.FlowOrderControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@CrossOrigin
class OrderController(
    private val service: OrderService,
    private val converter: OrderToDtoConverter
) : FlowOrderControllerApi, FlowBidOrderControllerApi {

    override suspend fun getOrderByOrderId(orderId: String): ResponseEntity<FlowOrderDto> =
        service.orderById(orderId)
            ?.let { converter.convert(it) }
            .okOr404IfNull()

    override suspend fun getOrdersAll(
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val sort = OrderFilter.Sort.LATEST_FIRST
        return result(
            service.findAllSell(continuation, size, sort),
            sort, size
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
            sorting, size
        )
    }

    override fun getOrdersByIds(flowOrderIdsDto: FlowOrderIdsDto): ResponseEntity<Flow<FlowOrderDto>> {
        return service.ordersByIds(flowOrderIdsDto.ids).map {
            converter.convert(it)
        }.okOr404IfNull()
    }

    override suspend fun getOrdersSync(
        continuation: String?,
        size: Int?,
        sort: String,
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val orderSort = OrderFilter.Sort.valueOf(sort)
        return result(
            service.findAll(continuation, size, orderSort),
            orderSort, size
        )
    }

    override fun getSellCurrencies(itemId: String): ResponseEntity<Flow<FlowAssetDto>> {
        return service.sellCurrenciesByItemId(itemId.itemId()).map {
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
            service.findAllSell(continuation, size, sort),
            sort, size
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
            sort, size
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
        val sort = OrderFilter.Sort.AMOUNT_ASC
        return result(
            service.getSellOrdersByItemAndStatus(
                itemId, makerAddress, null, emptyList(), continuation, size, sort
            ),
            sort, size
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
        val itemId = ItemId(contract, tokenId.tokenId())
        val sort = OrderFilter.Sort.AMOUNT_ASC
        val orderStatuses = OderStatusDtoConverter.convert(status)
        return result(
            service.getSellOrdersByItemAndStatus(
                itemId, makerAddress, currencyAddress, orderStatuses, continuation, size, sort
            ),
            sort, size
        )
    }

    override suspend fun getSellOrdersByMaker(
        maker: List<String>,
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val makerAddresses = maker.mapNotNull { it.flowAddress() }
        val sort = OrderFilter.Sort.LATEST_FIRST
        return result(
            service.getSellOrdersByMaker(makerAddresses, continuation, size, sort),
            sort, size
        )
    }

    override fun getBidCurrencies(itemId: String): ResponseEntity<Flow<FlowAssetDto>> {
        return service.bidCurrenciesByItemId(itemId.itemId()).map {
            FlowAssetFungibleDto(it.contract, it.value)
        }.okOr404IfNull()
    }

    override suspend fun getBidsByItem(
        contract: String,
        tokenId: String,
        status: List<FlowOrderStatusDto>,
        maker: List<String>?,
        origin: String?,
        startDate: Instant?,
        endDate: Instant?,
        currencyAddress: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val makers = maker?.map { it.flowAddress()!! } ?: emptyList()
        val itemId = ItemId(contract, tokenId.tokenId())
        val sort = OrderFilter.Sort.AMOUNT_DESC
        val orderStatuses = OderStatusDtoConverter.convert(status)
        return result(
            service.getBidOrdersByItem(
                itemId,
                makers,
                currencyAddress,
                orderStatuses,
                startDate,
                endDate,
                continuation,
                size,
                sort
            ),
            sort, size
        )
    }

    override suspend fun getOrderBidsByMaker(
        maker: List<String>,
        origin: String?,
        status: List<FlowOrderStatusDto>?,
        startDate: Instant?,
        endDate: Instant?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val makerAddresses = maker.mapNotNull { it.flowAddress() }
        val sort = OrderFilter.Sort.AMOUNT_DESC
        val orderStatuses = OderStatusDtoConverter.convert(status)
        return result(
            service.getBidOrdersByMaker(
                makerAddresses,
                orderStatuses,
                origin,
                startDate,
                endDate,
                continuation,
                size,
                sort
            ),
            sort, size
        )
    }

    private suspend fun result(
        orders: Flow<Order>,
        sort: OrderFilter.Sort,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        return converter.page(orders, sort, size).okOr404IfNull()
    }
}
