package com.rarible.flow.scanner.service.order

import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.util.offchainEventMarks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@ExperimentalCoroutinesApi
class OrderStartEndCheckerHandler(
    private val orderRepository: OrderRepository,
    private val orderConverter: OrderToDtoConverter,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderStartedMetric: RegisteredCounter,
    private val orderExpiredMetric: RegisteredCounter
) : JobHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle() {
        update(Instant.now())
    }

    suspend fun update(now: Instant) {
        logger.info("Starting to update status for orders...")

        merge(
            orderRepository.findExpiredOrders(now),
            orderRepository.findNotStartedOrders(now)
        ).collect { order ->
            val marks = offchainEventMarks()
            val saved = orderRepository.save(order.withUpdatedStatus(now)).awaitSingle()
            if (order.isEnded()) orderExpiredMetric.increment() else orderStartedMetric.increment()
            logger.info("Changed order ${saved.id} status to ${saved.status}")
            protocolEventPublisher.onOrderUpdate(saved, orderConverter, marks)
        }
    }
}
