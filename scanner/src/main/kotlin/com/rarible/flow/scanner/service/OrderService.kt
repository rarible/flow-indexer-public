package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class OrderService(
    val orderRepository: OrderRepository,
    val currencyApi: CurrencyControllerApi
) {
    val logger by Log()

    suspend fun list(activity: FlowNftOrderActivityList, item: Item?): Order {
        val status = if (item == null || item.owner?.formatted != activity.maker) OrderStatus.INACTIVE else OrderStatus.ACTIVE

        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            itemId = ItemId(activity.make.contract, activity.tokenId),
            maker = FlowAddress(activity.maker),
            make = activity.make,
            take = activity.take,
            amount = activity.price,
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            collection = activity.contract,
            makeStock = activity.make.value.toBigInteger(),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.LIST
        ) ?: Order(
            id = activity.hash.toLong(),
            status = status,
            itemId = ItemId(activity.make.contract, activity.tokenId),
            maker = FlowAddress(activity.maker),
            make = activity.make,
            take = activity.take,
            amount = activity.price,
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            collection = activity.contract,
            makeStock = activity.make.value.toBigInteger(),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.LIST
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
            data = OrderData(
                activity.payments.filter {
                    it.type !in arrayOf(
                        PaymentType.SELLER_FEE,
                        PaymentType.BUYER_FEE
                    )
                }.map { Payout(account = FlowAddress(it.address), value = it.amount) },
                activity.payments.filter { it.type in arrayOf(PaymentType.SELLER_FEE, PaymentType.BUYER_FEE) }
                    .map { Payout(account = FlowAddress(it.address), value = it.amount) }),
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
            make = activity.left.asset,
            take = activity.right.asset,
            data = OrderData(
                payouts = activity.payments.filter {
                    it.type !in arrayOf(
                        PaymentType.SELLER_FEE,
                        PaymentType.BUYER_FEE
                    )
                }.map { Payout(account = FlowAddress(it.address), value = it.amount) },
                originalFees = activity.payments.filter { it.type in arrayOf(PaymentType.SELLER_FEE, PaymentType.BUYER_FEE) }
                    .map { Payout(account = FlowAddress(it.address), value = it.amount) }),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.LIST
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
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            itemId = ItemId("", 0L),
            amount = BigDecimal.ZERO,
            collection = "",
            maker = FlowAddress("0x00"),
            make = FlowAssetEmpty,
            take = FlowAssetEmpty,
            data = OrderData(emptyList(), emptyList()),
            makeStock = BigInteger.ZERO,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.LIST
        )

        return orderRepository.coSave(order)
    }

    suspend fun deactivateOrdersByItem(item: Item, before: LocalDateTime): List<Order> {
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
            .asFlow().toList()
    }

    suspend fun restoreOrdersForItem(item: Item, before: LocalDateTime): List<Order> = orderRepository
        .findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
            FlowAssetNFT(item.contract, BigDecimal.ONE, item.tokenId),
            item.owner!!,
            OrderStatus.INACTIVE,
            before
        )
        .flatMap {
            orderRepository.save(it.copy(status = OrderStatus.ACTIVE))
        }
        .asFlow().toList()


    suspend fun updateOrdersPrices() {
        orderRepository
            .findAllByStatus(OrderStatus.ACTIVE)
            .subscribe { order ->
                val take = order.take
                getRate(take).subscribe { rate ->
                    val takePriceUsd = take.value * rate
                    orderRepository.save(
                        order.copy(takePriceUsd = takePriceUsd)
                    ).subscribe {
                        logger.info("Order's [{}] takePriceUsd is updated to {}", order.id, takePriceUsd)
                    }
                }
            }
    }

    private fun getRate(take: FlowAsset): Mono<BigDecimal> {
        return if (take is FlowAssetFungible) {
            currencyApi
                .getCurrencyRate(BlockchainDto.FLOW, take.contract, Instant.now().toEpochMilli())
                .map {
                    it.rate
                }
        } else Mono.empty()
    }

}
