package com.rarible.flow.scanner.service

import com.rarible.flow.core.domain.Balance
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.UpdateDefinition
import org.springframework.data.mongodb.core.query.gt
import org.springframework.stereotype.Component
import org.springframework.data.mongodb.core.query.lte
import java.awt.Cursor
import java.math.BigDecimal
import kotlin.reflect.KProperty

@Component
class BidService(
    private val orderRepository: OrderRepository
) {
    val logger by Log()

    suspend fun deactivateBidsByBalance(balance: Balance): Flow<Order> {
        val deactivated = updateByFilter(
            match(OrderStatus.ACTIVE, balance, OrderFilter.ByMakeValue.Comparator.GT),
            { filter, cursor -> orderRepository.search(filter, cursor, 1000).asFlow() }
        ) { order -> orderRepository.coSave(order.deactivateBid(balance.balance)) }

        val ids = deactivated.map { it.id }.toList()
        logger.info("Deactivated orders by balance {}: {}", balance, ids)
        return deactivated
    }

    suspend fun activateBidsByBalance(balance: Balance): Flow<Order> {
        val reactivated = updateByFilter(
            match(OrderStatus.INACTIVE, balance, OrderFilter.ByMakeValue.Comparator.LTE),
            { filter, cursor -> orderRepository.search(filter, cursor, 1000).asFlow() }
        ) { order -> orderRepository.coSave(order.reactivateBid()) }

        val ids = reactivated.map { it.id }
        logger.info("Deactivated orders by balance {}: {}", balance, ids)
        return reactivated
    }

    private suspend fun updateByFilter(
        filter: OrderFilter,
        search: (OrderFilter, String?) -> Flow<Order>,
        fn: suspend (Order) -> Order
    ): Flow<Order> {
        return updateByFilter(filter, null, emptyFlow(), search, fn)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun updateByFilter(
        filter: OrderFilter,
        cursor: String?,
        collected: Flow<Order>,
        search: (OrderFilter, String?) -> Flow<Order>,
        fn: suspend (Order) -> Order
    ): Flow<Order> {
        val orders = search(filter, cursor)
        return if(orders.count() == 0) {
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