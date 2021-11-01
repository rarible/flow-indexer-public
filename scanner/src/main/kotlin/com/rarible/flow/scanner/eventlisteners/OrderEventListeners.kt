package com.rarible.flow.scanner.eventlisteners

import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.kafka.ProtocolEventPublisher
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

    @EventListener(FlowNftOrderActivityList::class)
    fun list(activity: FlowNftOrderActivityList): Unit = runBlocking {
        protocolEventPublisher.onUpdate(
            orderService.list(activity)
        )
    }

    @EventListener(FlowNftOrderActivitySell::class)
    fun orderClose(activity: FlowNftOrderActivitySell): Unit = runBlocking {
        protocolEventPublisher.onUpdate(
            orderService.close(activity)
        )
    }

    @EventListener(FlowNftOrderActivityCancelList::class)
    fun orderCancelled(activity: FlowNftOrderActivityCancelList): Unit = runBlocking {
        protocolEventPublisher.onUpdate(
            orderService.cancel(activity)
        )
    }

    @EventListener
    fun makeOrderInactive(event: ItemIsWithdrawn): Unit = runBlocking {
        protocolEventPublisher.onUpdate(
            orderService.deactivateOrdersByItem(
                event.item,
                LocalDateTime.ofInstant(event.activityTime, ZoneOffset.UTC)
            )
        )
    }

    @EventListener
    fun restoreOrders(event: ItemIsDeposited): Unit = runBlocking {
        val item = event.item
        protocolEventPublisher.onUpdate(
            orderService.restoreOrdersForItem(item)
        )
    }
}
