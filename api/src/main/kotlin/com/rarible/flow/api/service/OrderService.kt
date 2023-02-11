package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant


@Service
class OrderService(
    private val orderRepository: OrderRepository
) {

    suspend fun orderById(orderId: Long): Order? {
        return orderRepository.coFindById(orderId)
    }

    fun getSellOrdersByMaker(
        makerAddresses: List<FlowAddress>,
        cont: String?,
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByMakers(makerAddresses), cont, size, sort
        ).asFlow()
    }

    fun findAll(cont: String?, size: Int?, sort: OrderFilter.Sort): Flow<Order> {
        return orderRepository.search(
            OrderFilter.All, cont, size, sort
        ).asFlow()
    }

    fun findAllSell(cont: String?, size: Int?, sort: OrderFilter.Sort): Flow<Order> {
        return orderRepository.search(
            sellOrders(OrderFilter.All), cont, size, sort
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
            sellOrders(OrderFilter.ByStatus(status)), cont, size, sort
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
            sellOrders(
                OrderFilter.ByItemId(itemId),
                OrderFilter.ByMaker(makerAddress),
                OrderFilter.ByStatus(status),
                OrderFilter.BySellingCurrency(currency)
            ),
            continuation,
            size,
            sort
        ).asFlow()
    }

    fun sellCurrenciesByItemId(itemId: ItemId): Flow<FlowAssetFungible> {
        return orderRepository
            .findAllByMake(itemId.contract, itemId.tokenId)
            .asFlow()
            .map { it.take.contract }
            .distinctUntilChanged()
            .map {
                FlowAssetFungible(it, BigDecimal.ZERO)
            }
    }

    fun bidCurrenciesByItemId(itemId: ItemId): Flow<FlowAssetFungible> {
        return orderRepository
            .findAllByTake(itemId.contract, itemId.tokenId)
            .asFlow()
            .map { it.make.contract }
            .distinctUntilChanged()
            .map {
                FlowAssetFungible(it, BigDecimal.ZERO)
            }
    }

    private fun sellOrders(vararg filters: OrderFilter): OrderFilter {
        return filters.foldRight(OrderFilter.OnlySell as OrderFilter) { filter, acc -> filter * acc }
    }

    private fun bidOrders(vararg filters: OrderFilter): OrderFilter {
        return filters.foldRight(OrderFilter.OnlyBid as OrderFilter) { filter, acc -> filter * acc }
    }

    fun getBidOrdersByItem(
        itemId: ItemId,
        makers: List<FlowAddress>,
        currency: String?,
        status: List<OrderStatus>,
        startDate: Instant?,
        endDate: Instant?,
        continuation: String?,
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            bidOrders(
                OrderFilter.ByItemId(itemId),
                OrderFilter.ByMakers(makers),
                OrderFilter.ByStatus(status),
                OrderFilter.ByBiddingCurrency(currency),
                OrderFilter.ByDateAfter(Order::createdAt, startDate),
                OrderFilter.ByDateBefore(Order::createdAt, endDate)
            ),
            continuation,
            size,
            sort
        ).asFlow()
    }

    fun getBidOrdersByMaker(
        makerAddresses: List<FlowAddress>,
        status: List<OrderStatus>,
        origin: String?,
        startDate: Instant?,
        endDate: Instant?,
        continuation: String?,
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            bidOrders(
                OrderFilter.ByMakers(makerAddresses),
                OrderFilter.ByStatus(status),
                OrderFilter.ByDateAfter(Order::createdAt, startDate),
                OrderFilter.ByDateBefore(Order::createdAt, endDate)
            ),
            continuation,
            size,
            sort
        ).asFlow()
    }
}
