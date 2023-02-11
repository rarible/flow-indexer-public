package com.rarible.flow.scanner.service

import com.rarible.flow.core.domain.Balance
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class BidService(
    private val orderRepository: OrderRepository
) {
    val logger by Log()

    suspend fun deactivateBidsByBalance(balance: Balance): Flow<Order> {
        return updateByFilter(
            match(OrderStatus.ACTIVE, balance, OrderFilter.ByMakeValue.Comparator.GT),
            { filter, cursor -> orderRepository.search(filter, cursor, 1000).asFlow() }
        ) { order -> orderRepository.coSave(order.deactivateBid(balance.balance)) }
    }

    suspend fun activateBidsByBalance(balance: Balance): Flow<Order> {
        return updateByFilter(
            match(OrderStatus.INACTIVE, balance, OrderFilter.ByMakeValue.Comparator.LTE),
            { filter, cursor -> orderRepository.search(filter, cursor, 1000).asFlow() }
        ) { order -> orderRepository.coSave(order.reactivateBid()) }
    }

    private suspend fun updateByFilter(
        filter: OrderFilter,
        search: (OrderFilter, String?) -> Flow<Order>,
        fn: suspend (Order) -> Order
    ): Flow<Order> {
        return updateByFilter(filter, null, emptyFlow(), search, fn)
    }

    private suspend fun updateByFilter(
        filter: OrderFilter,
        cursor: String?,
        collected: Flow<Order>,
        search: (OrderFilter, String?) -> Flow<Order>,
        fn: suspend (Order) -> Order
    ): Flow<Order> {
        logger.debug("Searching bids by filter: {}; cursor: {}", filter.criteria().criteriaObject, cursor)
        val orders = search(filter, cursor)
        return if(orders.count() == 0) {
            logger.debug("Returning bids for filter: {}", filter.criteria().criteriaObject)
            collected
        } else {
            val nextCollected = merge(collected, orders.map(fn))
            val nextCursor = OrderFilter.Sort.LATEST_FIRST.nextPage(orders.last())
            updateByFilter(filter, nextCursor, nextCollected, search, fn)
        }
    }

    private fun match(
        status: OrderStatus, balance: Balance, comparison: OrderFilter.ByMakeValue.Comparator
    ): OrderFilter {
        return OrderFilter.OnlyBid *
                OrderFilter.ByStatus(status) *
                OrderFilter.ByMaker(balance.account) *
                OrderFilter.ByMakeValue(comparison, balance.balance)
    }
}
