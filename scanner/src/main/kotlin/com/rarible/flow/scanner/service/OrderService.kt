package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import org.springframework.stereotype.Service
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

}