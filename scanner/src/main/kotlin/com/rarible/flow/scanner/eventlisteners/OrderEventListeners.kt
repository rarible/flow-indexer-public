package com.rarible.flow.scanner.eventlisteners

import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.scanner.ProtocolEventPublisher
import com.rarible.flow.scanner.service.OrderService
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class OrderEventListeners(
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderService: OrderService
) {

    @EventListener(FlowNftOrderActivityList::class)
    fun list(activity: FlowNftOrderActivityList) = runBlocking {
        protocolEventPublisher.onUpdate(
            orderService.list(activity)
        )
    }

    @EventListener(FlowNftOrderActivitySell::class)
    fun orderClose(activity: FlowNftOrderActivitySell) = runBlocking {
        protocolEventPublisher.onUpdate(
            orderService.close(activity)
        )
    }

    @EventListener(FlowNftOrderActivityCancelList::class)
    fun orderCancelled(activity: FlowNftOrderActivityCancelList) = runBlocking {
        protocolEventPublisher.onUpdate(
            orderService.cancel(activity)
        )
    }
}
