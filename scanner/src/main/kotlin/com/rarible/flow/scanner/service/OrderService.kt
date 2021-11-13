package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class OrderService(
    val orderRepository: OrderRepository
) {

    suspend fun list(activity: FlowNftOrderActivityList): Order {
        val originalFees = activity.payments
            .filter { it.type in setOf(PaymentType.BUYER_FEE, PaymentType.SELLER_FEE) }
            .map { Payout(FlowAddress(it.address), it.amount) } //TODO replace amount with share
        val payouts = activity.payments
            .filter { it.type in setOf(PaymentType.ROYALTY, PaymentType.OTHER, PaymentType.REWARD) }
            .map { Payout(FlowAddress(it.address), it.amount) } //TODO replace amount with share

        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            itemId = ItemId(activity.make.contract, activity.tokenId),
            maker = FlowAddress(activity.maker),
            make = activity.make,
            take = activity.take,
            amount = activity.price,
            data = OrderData(payouts, originalFees),
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            collection = activity.contract,
            makeStock = activity.make.value.toBigInteger(),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        ) ?: Order(
            id = activity.hash.toLong(),
            status = OrderStatus.ACTIVE,
            itemId = ItemId(activity.make.contract, activity.tokenId),
            maker = FlowAddress(activity.maker),
            make = activity.make,
            take = activity.take,
            amount = activity.price,
            data = OrderData(payouts, originalFees),
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            collection = activity.contract,
            makeStock = activity.make.value.toBigInteger(),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        )

        return orderRepository.coSave(order)
    }

    //TODO tests
    suspend fun close(activity: FlowNftOrderActivitySell): Order {
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
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            maker = FlowAddress(activity.left.maker),
            itemId = ItemId(activity.left.asset.contract, activity.tokenId),
            amount = activity.price,
            collection = activity.contract,
            make = FlowAssetEmpty,
            take = FlowAssetEmpty,
            data = OrderData(emptyList(), emptyList()),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        )

        return orderRepository.coSave(order)
    }

    //TODO tests
    suspend fun cancel(activity: FlowNftOrderActivityCancelList): Order {
        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            cancelled = true,
            status = OrderStatus.CANCELLED,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        ) ?: Order(
            id = activity.hash.toLong(),
            cancelled = true,
            status = OrderStatus.CANCELLED,
            createdAt =  LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
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

        return orderRepository.coSave(order)
    }

    fun deactivateOrdersByItem(item: Item, before: LocalDateTime): Flow<Order> {
        return orderRepository
            .findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
                FlowAssetNFT(item.contract, 1.toBigDecimal(), item.tokenId),
                item.owner!!,
                OrderStatus.ACTIVE,
                before,
            )
            .flatMap {
                orderRepository.save(it.copy(status = OrderStatus.INACTIVE))
            }
            .asFlow()
    }

    // TODO we can restore all past orders
    fun restoreOrdersForItem(item: Item): Flow<Order> {
//        orderRepository
//            .findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
//                FlowAssetNFT(activity.contract, 1.toBigDecimal(), activity.tokenId),
//                FlowAddress(event.to),
//                OrderStatus.INACTIVE,
//                LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
//            )
//            .map {
//                it.copy(status = OrderStatus.ACTIVE)
//            }
        return emptyFlow()
    }
}
