package com.rarible.flow.api.controller

import com.rarible.flow.api.service.OrderService
import com.rarible.flow.core.converter.OderStatusDtoConverter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.flow.nft.api.controller.FlowBidOrderControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@CrossOrigin
class BidApiController(
    private val service: OrderService,
    private val converter: OrderToDtoConverter
): FlowBidOrderControllerApi {

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
        maker: String,
        status: List<FlowOrderStatusDto>,
        origin: String?,
        startDate: Instant?,
        endDate: Instant?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowOrdersPaginationDto> {
        val makerAddress = maker.flowAddress()
        val sort = OrderFilter.Sort.AMOUNT_DESC
        val orderStatuses = OderStatusDtoConverter.convert(status)
        return result(
            service.getBidOrdersByMaker(
                makerAddress,
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