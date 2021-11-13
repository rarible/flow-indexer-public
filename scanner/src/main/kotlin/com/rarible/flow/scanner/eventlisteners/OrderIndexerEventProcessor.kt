package com.rarible.flow.scanner.eventlisteners

import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.OrderService
import org.springframework.stereotype.Component

@Component
class OrderIndexerEventProcessor(
    private val orderService: OrderService,
    private val protocolEventPublisher: ProtocolEventPublisher,
): IndexerEventsProcessor {

    private val supportedTypes = arrayOf(FlowActivityType.LIST, FlowActivityType.SELL, FlowActivityType.CANCEL_LIST)

    override fun isSupported(event: IndexerEvent): Boolean = event.activityType() in supportedTypes

    override suspend fun process(event: IndexerEvent) {
        when(event.activityType()) {
            FlowActivityType.LIST -> list(event)
            FlowActivityType.SELL -> orderClose(event)
            FlowActivityType.CANCEL_LIST -> orderCancelled(event)
            else -> throw IllegalStateException("Unsupported order event! [${event.activityType()}]")
        }
    }

    private suspend fun list(event: IndexerEvent) {
        val activity = event.history.first().activity as FlowNftOrderActivityList
        val o = orderService.list(activity)
        if (event.source != Source.REINDEX) {
            protocolEventPublisher.onUpdate(o)
        }
    }

    private suspend fun orderClose(event: IndexerEvent) {
        val activity = event.history.first().activity as FlowNftOrderActivitySell
        val o = orderService.close(activity)
        if (event.source != Source.REINDEX) {
            protocolEventPublisher.onUpdate(o)
        }
    }

    private suspend fun orderCancelled(event: IndexerEvent) {
        val activity = event.history.first().activity as FlowNftOrderActivityCancelList
        val o = orderService.cancel(activity)
        if (event.source != Source.REINDEX) {
            protocolEventPublisher.onUpdate(o)
        }
    }
}
