package com.rarible.flow.scanner.eventlisteners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.scanner.ProtocolEventPublisher
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class OrderEventListeners(
    private val orderRepository: OrderRepository,
    private val protocolEventPublisher: ProtocolEventPublisher
) {

    @EventListener(FlowNftOrderActivityList::class)
    fun list(activity: FlowNftOrderActivityList) = runBlocking {
        val order = orderRepository.coSave(
            Order(
            activity.hash.toLong(),
            ItemId(activity.contract, activity.tokenId),
            maker = FlowAddress(activity.maker),
            taker = null,
            make = activity.make,
            take = activity.take,
            amount = activity.price,
            data = OrderData(emptyList(), emptyList()), //TODO
            collection = activity.contract,
            makeStock = activity.make.value.toBigInteger()
            )
        )

        protocolEventPublisher.onUpdate(order)
    }

    @EventListener(FlowNftOrderActivityCancelList::class)
    fun cancelList(activity: FlowNftOrderActivityCancelList) = runBlocking {
        val order = orderRepository.coFindById(activity.hash.toLong())

        if(order != null) {
            protocolEventPublisher.onUpdate(
                orderRepository.coSave(order.copy(cancelled = true))
            )
        }
    }

    @EventListener(FlowNftOrderActivitySell::class)
    fun completeOrder(activity: FlowNftOrderActivitySell) = runBlocking {
        //TODO
    }

}