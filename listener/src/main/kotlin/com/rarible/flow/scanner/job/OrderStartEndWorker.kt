package com.rarible.flow.scanner.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.scanner.config.StartEndWorkerProperties
import com.rarible.protocol.dto.offchainEventMark
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.time.delay
import java.time.Instant

@ExperimentalCoroutinesApi
class OrderStartEndWorker(
    private val orderRepository: OrderRepository,
    private val orderConverter: OrderToDtoConverter,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val properties: StartEndWorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "start-end-orders-check-job"
) {

    override suspend fun handle() {
        update(Instant.now())
        delay(properties.pollingPeriod)
    }

    suspend fun update(now: Instant) {
        logger.info("Starting to update status for orders...")

        merge(
            orderRepository.findExpiredOrders(now),
            orderRepository.findNotStartedOrders(now)
        ).collect { order ->
            val saved = orderRepository.save(order.withUpdatedStatus(now)).awaitSingle()
            logger.info("Change order ${saved.id} status to ${saved.status}")
            protocolEventPublisher.onOrderUpdate(saved, orderConverter, offchainEventMark("indexer-out_order"))
        }
    }

}
