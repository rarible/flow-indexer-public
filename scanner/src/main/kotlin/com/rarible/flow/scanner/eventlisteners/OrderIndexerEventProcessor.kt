package com.rarible.flow.scanner.eventlisteners

import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.core.apm.withSpan
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

    override fun isSupported(event: IndexerEvent): Boolean = event.activity is FlowNftOrderActivityList ||
            event.activity is FlowNftOrderActivitySell || event.activity is FlowNftOrderActivityCancelList

    override suspend fun process(event: IndexerEvent) {
        when(event.activity) {
            is FlowNftOrderActivityList -> list(event)
            is FlowNftOrderActivitySell -> orderClose(event)
            is FlowNftOrderActivityCancelList -> orderCancelled(event)
            else -> throw IllegalStateException("Unsupported order event! [${event.activity::class.simpleName}]")
        }
    }

    private suspend fun list(event: IndexerEvent) {
        val activity = event.activity as FlowNftOrderActivityList
        withSpan("listOrderEvent", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val o = orderService.list(activity)
            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onUpdate(o)
            }
        }
    }

    private suspend fun orderClose(event: IndexerEvent) {
        val activity = event.activity as FlowNftOrderActivitySell
        withSpan("closeOrderEvent", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val o = orderService.close(activity)
            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onUpdate(o)
            }
        }
    }

    private suspend fun orderCancelled(event: IndexerEvent) {
        val activity = event.activity as FlowNftOrderActivityCancelList
        withSpan("cancelOrderEvent", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val o = orderService.cancel(activity)
            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onUpdate(o)
            }
        }
    }
}
