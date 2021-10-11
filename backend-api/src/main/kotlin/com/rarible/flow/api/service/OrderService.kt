package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
        cont: ActivityContinuation?,
        size: Int?
    ): List<FlowOrderDto> {
        return convert(
            orderRepository.search(
                OrderFilter.ByMaker(makerAddress, originAddress), cont, size
            )
        )
    }

    suspend fun findAll(cont: ActivityContinuation?, size: Int?): List<FlowOrderDto> {
        return convert(
            orderRepository.search(
                OrderFilter.All, cont, size
            )
        )
    }

    suspend fun getSellOrdersByCollection(
        collection: String,
        cont: ActivityContinuation?,
        size: Int?
    ): List<FlowOrderDto> {
        return convert(
            orderRepository.search(
                OrderFilter.ByCollection(collection), cont, size
            )
        )
    }

    private suspend fun convert(orders: Flow<Order>): List<FlowOrderDto> {
        return orders.map {
            OrderToDtoConverter.convert(it)
        }.toList()
    }

    suspend fun findAllByStatus(status: List<FlowOrderStatusDto>, cont: ActivityContinuation?, size: Int?): List<FlowOrderDto> {
        return convert(
            orderRepository.search(
                OrderFilter.ByStatus(status), cont, size
            )
        )
    }
}
