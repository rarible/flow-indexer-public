package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.math.BigDecimal


@Service
class OrderService(
    private val orderRepository: OrderRepository
) {

    suspend fun orderById(orderId: Long): Order? {
        return orderRepository.coFindById(orderId)
    }

    fun getSellOrdersByMaker(
        makerAddress: FlowAddress,
        originAddress: FlowAddress?,
        cont: String?,
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByMaker(makerAddress, originAddress), cont, size, sort
        ).asFlow()
    }

    fun findAll(cont: String?, size: Int?, sort: OrderFilter.Sort): Flow<Order> {
        return orderRepository.search(
            OrderFilter.All, cont, size, sort
        ).asFlow()
    }

    fun getSellOrdersByCollection(
        collection: String,
        cont: String?,
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByCollection(collection), cont, size, sort
        ).asFlow()
    }

    fun findAllByStatus(
        status: List<OrderStatus>?,
        cont: String?,
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByStatus(status), cont, size, sort
        ).asFlow()
    }

    fun ordersByIds(ids: List<Long>): Flow<Order> {
        return orderRepository.findAllByIdIn(ids).asFlow()
    }

    fun getSellOrdersByItemAndStatus(
        itemId: ItemId,
        makerAddress: FlowAddress?,
        currency: String?,
        status: List<OrderStatus>,
        continuation: String?,
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByItemId(itemId) *
                    OrderFilter.ByMaker(makerAddress) *
                    OrderFilter.ByStatus(status) *
                    OrderFilter.ByCurrency(currency),
            continuation,
            size,
            sort
        ).asFlow()
    }

    fun currenciesByItemId(itemId: String): Flow<FlowAsset> = flow {
        val id = ItemId.parse(itemId)
        emitAll(orderRepository.findAllByMake(id.contract, id.tokenId).asFlow().map {
            FlowAssetFungible(it.take.contract, BigDecimal.ZERO)
        }.toSet().asFlow())
    }
}
