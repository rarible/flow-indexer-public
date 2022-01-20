package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSaveAll
import com.rarible.flow.core.repository.forEach
import com.rarible.flow.log.Log
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.runBlocking

@ChangeUnit(
    id = "ChangeLog00013PriceUsdConversion",
    order = "00013",
    author = "flow"
)
class ChangeLog00013PriceUsdConversion(
    private val orderRepository: OrderRepository,
    private val itemHistoryRepository: ItemHistoryRepository
) {

    private val logger by Log()

    @Execution
    fun changeSet() {
        runBlocking {
            orderRepository.forEach(
                OrderFilter.OnlySell, null, 1000
            ) { order ->
                val sellHistory = itemHistoryRepository
                    .findOrderActivity(
                        FlowActivityType.SELL.name, order.id.toString()
                    )
                    .collectList()
                    .awaitFirstOrDefault(emptyList())
                logger.info("Found {} sell entries for order #{}", sellHistory.size, order.id)
                itemHistoryRepository.coSaveAll(sellHistory)
                logger.info("Saved history for order #{}", order.id)
            }
        }
    }

    @RollbackExecution
    fun rollBack() {

    }
}
