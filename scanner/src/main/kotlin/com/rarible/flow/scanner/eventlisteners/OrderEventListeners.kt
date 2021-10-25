package com.rarible.flow.scanner.eventlisteners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.scanner.ProtocolEventPublisher
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class OrderEventListeners(
    private val orderRepository: OrderRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
) {

    @EventListener(FlowNftOrderActivityList::class)
    fun list(activity: FlowNftOrderActivityList) = runBlocking {
        val originalFees = activity.payments
            .filter { it.type in setOf(PaymentType.BUYER_FEE, PaymentType.SELLER_FEE) }
            .map { Payout(FlowAddress(it.address), it.amount) }
        val payouts = activity.payments
            .filter { it.type in setOf(PaymentType.ROYALTY, PaymentType.OTHER) }
            .map { Payout(FlowAddress(it.address), it.amount) }

        val order = orderRepository.coSave(
            Order(
                activity.hash.toLong(),
                ItemId(activity.contract, activity.tokenId),
                maker = FlowAddress(activity.maker),
                taker = null,
                make = activity.make,
                take = activity.take,
                amount = activity.price,
                data = OrderData(payouts, originalFees),
                collection = activity.contract,
                makeStock = activity.make.value.toBigInteger(),
                status = OrderStatus.ACTIVE,
                lastUpdatedAt = LocalDateTime.now(ZoneOffset.UTC)
            )
        )

        protocolEventPublisher.onUpdate(order)
    }

    @EventListener(FlowNftOrderActivityCancelList::class)
    fun cancelList(activity: FlowNftOrderActivityCancelList) = runBlocking {
        val order = orderRepository.coFindById(activity.hash.toLong())

        if (order != null) {
            protocolEventPublisher.onUpdate(
                orderRepository.coSave(order.copy(cancelled = true, status = OrderStatus.CANCELLED))
            )
        }
    }

    @EventListener(FlowNftOrderActivitySell::class)
    fun completeOrder(activity: FlowNftOrderActivitySell) = runBlocking {
        val order = orderRepository.coFindById(activity.hash.toLong())

        if (order != null) {
            protocolEventPublisher.onUpdate(
                orderRepository.coSave(order.copy(
                    taker = FlowAddress(activity.right.maker),
                    status = OrderStatus.FILLED
                ))
            )
        }
    }
}
