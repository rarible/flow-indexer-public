package com.rarible.flow.scanner.service

import com.rarible.flow.core.domain.Balance
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.log.Log
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.UpdateDefinition
import org.springframework.data.mongodb.core.query.gt
import org.springframework.stereotype.Component
import org.springframework.data.mongodb.core.query.lte
import java.math.BigDecimal
import kotlin.reflect.KProperty

@Component
class BidService(
    private val orderRepository: OrderRepository
) {

    suspend fun deactivateBidsByBalance(balance: Balance) {
        val result = orderRepository.update(
            match(OrderStatus.ACTIVE, balance, KProperty<BigDecimal>::gt),
            activationUpdate(OrderStatus.INACTIVE, balance.balance)
        )

        if (result.wasAcknowledged()) {
            logger.info("Deactivated bids by balance {} with result {}", balance, result)
        } else {
            logger.warn("Failed to deactivate bids for {}", balance)
        }
    }

    suspend fun activateBidsByBalance(balance: Balance) {
        val result = orderRepository.update(
            match(OrderStatus.INACTIVE, balance, KProperty<BigDecimal>::lte),
            activationUpdate(OrderStatus.ACTIVE, balance.balance)
        )

        if (result.wasAcknowledged()) {
            logger.info("Activated bids by balance {} with result {}", balance, result)
        } else {
            logger.warn("Failed to activate bids for {}", balance)
        }
    }

    private fun match(
        status: OrderStatus, balance: Balance, comparison: KProperty<BigDecimal>.(BigDecimal) -> Criteria
    ): OrderFilter {
        return OrderFilter.OnlyBid *
                OrderFilter.ByStatus(status) *
                OrderFilter.ByMaker(balance.account) *
                OrderFilter.ByMakeValue(comparison, balance.balance)
    }

    private fun activationUpdate(newStatus: OrderStatus, newMakeStock: BigDecimal): UpdateDefinition {
        return Update()
            .set(Order::status.name, newStatus)
            .set(Order::makeStock.name, newMakeStock)
    }

    companion object {
        val logger by Log()
    }
}