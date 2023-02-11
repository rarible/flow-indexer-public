package com.rarible.flow.scanner.job

import com.rarible.flow.core.util.Log
import com.rarible.flow.scanner.service.OrderService
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "jobs.orderPriceUpdate", name = ["enabled"], havingValue = "true", matchIfMissing = false )
class OrderPricesUpdateJob(
    private val orderService: OrderService,
) {

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${jobs.orderPriceUpdate.rate}")
    fun updateOrdersPrices() = runBlocking {
        logger.info("Starting updateOrdersPrices()...")
        orderService.updateOrdersPrices()
        logger.info("Successfully updated order prices.")
    }

    companion object {
        val logger by Log()
    }
}