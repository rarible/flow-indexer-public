package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderConverter: OrderToDtoConverter,
    private val currencyApi: CurrencyControllerApi
) {
    val logger by Log()

    suspend fun openList(activity: FlowNftOrderActivityList, item: Item?): Order {
        val status =
            if (item == null || item.owner?.formatted != activity.maker) OrderStatus.INACTIVE else OrderStatus.ACTIVE

        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            itemId = ItemId(activity.make.contract, activity.tokenId),
            maker = FlowAddress(activity.maker),
            make = activity.make,
            take = activity.take,
            amount = activity.price,
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            collection = activity.contract,
            makeStock = activity.make.value,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.LIST,
            takePriceUsd = activity.priceUsd
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
            makeStock = activity.make.value,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.LIST,
            takePriceUsd = activity.priceUsd
        )

        return orderRepository.coSave(order)
    }

    suspend fun openBid(activity: FlowNftOrderActivityBid, item: Item?): Order {
        val status =
            if (item == null) OrderStatus.INACTIVE else OrderStatus.ACTIVE
        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            itemId = ItemId(activity.take.contract, activity.tokenId),
            maker = FlowAddress(activity.maker),
            make = activity.make,
            take = activity.take,
            amount = activity.price,
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            collection = activity.contract,
            makeStock = activity.make.value,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.BID,
            takePriceUsd = activity.priceUsd,
            status = status
        ) ?: Order(
            id = activity.hash.toLong(),
            status = status,
            itemId = ItemId(activity.take.contract, activity.tokenId),
            maker = FlowAddress(activity.maker),
            make = activity.make,
            take = activity.take,
            amount = activity.price,
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            collection = activity.contract,
            makeStock = activity.make.value,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.BID,
            takePriceUsd = activity.priceUsd
        )

        return orderRepository.coSave(order)
    }

    //TODO tests
    suspend fun close(activity: FlowNftOrderActivitySell): Order {
        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            fill = BigDecimal.ONE,
            makeStock = BigDecimal.ZERO,
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
            makeStock = BigDecimal.ZERO,
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
                    it.type == PaymentType.REWARD
                }.map { Payout(account = FlowAddress(it.address), value = it.amount) },
                originalFees = activity.payments.filter {
                    it.type in arrayOf(
                        PaymentType.SELLER_FEE,
                        PaymentType.BUYER_FEE
                    )

                }
                    .map { Payout(account = FlowAddress(it.address), value = it.amount) }),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.LIST,
            takePriceUsd = activity.priceUsd
        )

        return orderRepository.coSave(order)
    }

    suspend fun closeBid(activity: FlowNftOrderActivityBidAccept, item: Item?): Order {
        val order = orderRepository.coFindById(activity.hash.toLong())?.let {
            it.copy(
                fill = it.makeStock,
                makeStock = BigDecimal.ZERO,
                taker = FlowAddress(activity.left.maker),
                status = OrderStatus.FILLED,
                data = OrderData(
                    activity.payments.filter {
                        it.type == PaymentType.REWARD
                    }.map { Payout(account = FlowAddress(it.address), value = it.amount) },
                    activity.payments.filter { it.type in arrayOf(PaymentType.SELLER_FEE, PaymentType.BUYER_FEE) }
                        .map { Payout(account = FlowAddress(it.address), value = it.amount) }),
                lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            )
        } ?: Order(
            id = activity.hash.toLong(),
            fill = activity.right.asset.value,
            makeStock = BigDecimal.ZERO,
            taker = FlowAddress(activity.left.maker),
            status = OrderStatus.FILLED,
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            maker = FlowAddress(activity.right.maker),
            itemId = ItemId(activity.left.asset.contract, activity.tokenId),
            amount = activity.price,
            collection = activity.contract,
            make = activity.left.asset,
            take = activity.right.asset,
            data = OrderData(
                payouts = activity.payments.filter {
                    it.type == PaymentType.REWARD
                }.map { Payout(account = FlowAddress(it.address), value = it.amount) },
                originalFees = activity.payments.filter {
                    it.type in arrayOf(
                        PaymentType.SELLER_FEE,
                        PaymentType.BUYER_FEE
                    )
                }.map { Payout(account = FlowAddress(it.address), value = it.amount) }),
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.LIST,
            takePriceUsd = activity.priceUsd
        )

        return orderRepository.coSave(order)
    }

    //TODO tests
    suspend fun cancel(activity: FlowNftOrderActivityCancelList, item: Item?): Order {
        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            cancelled = true,
            status = OrderStatus.CANCELLED,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        ) ?: Order(
            id = activity.hash.toLong(),
            cancelled = true,
            status = OrderStatus.CANCELLED,
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            itemId = item?.id ?: ItemId("", 0L),
            amount = BigDecimal.ZERO,
            collection = item?.collection.orEmpty(),
            maker = item?.owner ?: FlowAddress("0x00"),
            make = if (item != null) {
                FlowAssetNFT(
                    contract = item.contract,
                    value = BigDecimal.ONE,
                    tokenId = item.tokenId
                )
            } else FlowAssetEmpty,
            take = FlowAssetEmpty,
            data = OrderData(emptyList(), emptyList()),
            makeStock = BigDecimal.ZERO,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.LIST,
        )

        return orderRepository.coSave(order)
    }

    suspend fun cancelBid(activity: FlowNftOrderActivityCancelBid, item: Item?): Order {
        val order = orderRepository.coFindById(activity.hash.toLong())?.copy(
            cancelled = true,
            status = OrderStatus.CANCELLED,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
        ) ?: Order(
            id = activity.hash.toLong(),
            cancelled = true,
            status = OrderStatus.CANCELLED,
            createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            itemId = item?.id ?: ItemId("", 0L),
            amount = BigDecimal.ZERO,
            collection = item?.collection.orEmpty(),
            maker = item?.owner ?: FlowAddress("0x00"),
            make = FlowAssetEmpty,
            take = if (item != null) {
                FlowAssetNFT(
                    contract = item.contract,
                    value = BigDecimal.ONE,
                    tokenId = item.tokenId
                )
            } else FlowAssetEmpty,
            data = OrderData(emptyList(), emptyList()),
            makeStock = BigDecimal.ZERO,
            lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
            type = OrderType.BID
        )

        return orderRepository.coSave(order)
    }

    suspend fun deactivateOrdersByOwnership(ownership: Ownership, before: Instant, needSendToKafka: Boolean): List<Order> = orderRepository
        .findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
            FlowAssetNFT(ownership.contract, BigDecimal.ONE, ownership.tokenId),
            ownership.owner,
            OrderStatus.ACTIVE,
            LocalDateTime.ofInstant(before, ZoneOffset.UTC),
        )
        .flatMap {
            orderRepository.save(it.copy(status = OrderStatus.INACTIVE))
        }
        .asFlow()
        .onEach {
            if (needSendToKafka) protocolEventPublisher.onOrderUpdate(it, orderConverter)
        }
        .toList()

    suspend fun restoreOrdersForOwnership(ownership: Ownership, before: Instant, needSendToKafka: Boolean): List<Order> = orderRepository
        .findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
            FlowAssetNFT(ownership.contract, BigDecimal.ONE, ownership.tokenId),
            ownership.owner,
            OrderStatus.INACTIVE,
            LocalDateTime.ofInstant(before, ZoneOffset.UTC),
        )
        .flatMap {
            orderRepository.save(it.copy(status = OrderStatus.ACTIVE))
        }
        .asFlow()
        .onEach {
            if (needSendToKafka) protocolEventPublisher.onOrderUpdate(it, orderConverter)
        }
        .toList()

    suspend fun deactivateOrdersByItem(item: Item, before: LocalDateTime): List<Order> {
        return orderRepository
            .findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
                FlowAssetNFT(item.contract, BigDecimal.ONE, item.tokenId),
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
