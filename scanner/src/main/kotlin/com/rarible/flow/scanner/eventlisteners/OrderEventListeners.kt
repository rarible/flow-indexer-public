package com.rarible.flow.scanner.eventlisteners

import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.OrderService
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class OrderEventListeners(
    private val orderService: OrderService,
    private val protocolEventPublisher: ProtocolEventPublisher,

    ) {

    @EventListener(condition = "#event.activity instanceof T(com.rarible.flow.core.domain.FlowNftOrderActivityList)")
    fun list(event: IndexerEvent): Unit = runBlocking {
        val activity = event.activity as FlowNftOrderActivityList
        val o = orderService.list(activity)
        if (event.source != Source.REINDEX) {
            protocolEventPublisher.onUpdate(o)
        }
    }

    @EventListener(condition = "#event.activity instanceof T(com.rarible.flow.core.domain.FlowNftOrderActivitySell)")
    fun orderClose(event: IndexerEvent): Unit = runBlocking {
        val activity = event.activity as FlowNftOrderActivitySell
        val o = orderService.close(activity)
        if (event.source != Source.REINDEX) {
            protocolEventPublisher.onUpdate(o)
        }
    }

    @EventListener(condition = "#event.activity instanceof T(com.rarible.flow.core.domain.FlowNftOrderActivityCancelList)")
    fun orderCancelled(event: IndexerEvent): Unit = runBlocking {
        val activity = event.activity as FlowNftOrderActivityCancelList
        val o = orderService.cancel(activity)
        if (event.source != Source.REINDEX) {
            protocolEventPublisher.onUpdate(o)
        }
    }

    @EventListener
    fun makeOrderInactive(event: ItemIsWithdrawn): Unit = runBlocking {
        val o = orderService.deactivateOrdersByItem(
            event.item,
            LocalDateTime.ofInstant(event.activityTime, ZoneOffset.UTC)
        )
        if (event.source != Source.REINDEX) {
            protocolEventPublisher.onUpdate(o)
        }
    }

    @EventListener
    fun restoreOrders(event: ItemIsDeposited): Unit = runBlocking {
        val item = event.item
        val o = orderService.restoreOrdersForItem(item)
        if (event.source != Source.REINDEX) {
            protocolEventPublisher.onUpdate(o)
        }
    }
}
