package com.rarible.flow.scanner.eventlisteners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.scanner.ProtocolEventPublisher
import com.rarible.flow.scanner.service.OrderService
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class OrderEventListeners(
    private val orderRepository: OrderRepository,
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
        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            fill = BigDecimal.ONE,
            makeStock = BigInteger.ZERO,
            taker = FlowAddress(activity.right.maker),
            status = OrderStatus.FILLED,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        ) ?: Order(
            id = activity.hash.toLong(),
            fill = BigDecimal.ONE,
            makeStock = BigInteger.ZERO,
            taker = FlowAddress(activity.right.maker),
            status = OrderStatus.FILLED,

            maker = FlowAddress(activity.left.maker),
            itemId = ItemId(activity.left.asset.contract, activity.tokenId),
            amount = activity.price,
            collection = activity.contract,
            make = FlowAssetEmpty,
            take = FlowAssetEmpty,
            data = OrderData(emptyList(), emptyList()),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        )

        protocolEventPublisher.onUpdate(orderRepository.coSave(order))
    }

    @EventListener(FlowNftOrderActivityCancelList::class)
    fun orderCancelled(activity: FlowNftOrderActivityCancelList) = runBlocking {

        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            cancelled = true,
            status = OrderStatus.CANCELLED,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        ) ?: Order(
            id = activity.hash.toLong(),
            cancelled = true,
            status = OrderStatus.CANCELLED,

            itemId = ItemId(activity.make.contract, activity.tokenId),
            amount = activity.price,
            collection = activity.contract,
            maker = FlowAddress(activity.maker),
            make = activity.make,
            take = activity.take,
            data = OrderData(emptyList(), emptyList()),
            makeStock = activity.make.value.toBigInteger(),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        )

        protocolEventPublisher.onUpdate(orderRepository.coSave(order))
    }
}
