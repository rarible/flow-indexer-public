package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service


@Service
class OrderService(
    private val orderRepository: OrderRepository
) {

    suspend fun orderById(orderId: Long): FlowOrderDto? {
        return orderRepository
            .coFindById(orderId)
            ?.let {
                OrderToDtoConverter.convert(it)
            }
    }

    suspend fun getSellOrdersByMaker(
        makerAddress: FlowAddress,
        originAddress: FlowAddress?,
        cont: String?,
        size: Int?
    ): List<FlowOrderDto> {
        return convert(
            orderRepository.search(
                OrderFilter.ByMaker(makerAddress, originAddress), cont, size, OrderFilter.Sort.LAST_UPDATE
            )
        )
    }

    suspend fun findAll(cont: String?, size: Int?): List<FlowOrderDto> {
        return convert(
            orderRepository.search(
                OrderFilter.All, cont, size, OrderFilter.Sort.LAST_UPDATE
            )
        )
    }

    suspend fun getSellOrdersByCollection(
        collection: String,
        cont: String?,
        size: Int?
    ): List<FlowOrderDto> {
        return convert(
            orderRepository.search(
                OrderFilter.ByCollection(collection), cont, size, OrderFilter.Sort.LAST_UPDATE
            )
        )
    }

    private suspend fun convert(orders: Flow<Order>): List<FlowOrderDto> {
        return orders.map {
            OrderToDtoConverter.convert(it)
        }.toList()
    }

    suspend fun findAllByStatus(
        status: List<FlowOrderStatusDto>,
        cont: String?,
        size: Int?
    ): List<FlowOrderDto> {
        return convert(
            orderRepository.search(
                OrderFilter.ByStatus(status), cont, size, OrderFilter.Sort.LAST_UPDATE
            )
        )
    }

    fun ordersByIds(ids: List<Long>): Flow<FlowOrderDto> {
        return orderRepository.findAllByIdIn(ids).asFlow().map {
            OrderToDtoConverter.convert(it)
        }
    }

    suspend fun getSellOrdersByItemAndStatus(
        itemId: ItemId,
        makerAddress: FlowAddress?,
        currency: FlowAddress?,
        status: List<FlowOrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): List<FlowOrderDto> {
        return convert(
            orderRepository.search(
                OrderFilter.ByItemId(itemId) *
                    OrderFilter.ByMaker(makerAddress) *
                    OrderFilter.ByStatus(status) *
                    OrderFilter.ByCurrency(currency),
                continuation,
                size,
                OrderFilter.Sort.MAKE_PRICE_ASC
            )
        )
    }
}
