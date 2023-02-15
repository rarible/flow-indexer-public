package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.FlowAssetEmpty
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowNftOrderActivityBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.domain.OrderType
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.util.Log
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.dto.FlowEventTimeMarksDto
import com.rarible.protocol.dto.FlowOrderPlatformDto
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderConverter: OrderToDtoConverter,
    private val currencyApi: CurrencyControllerApi,
) {

    private val logger by Log()

    suspend fun openList(activity: FlowNftOrderActivityList, item: Item?): Order {
        return withSpan("openList", "db") {
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

            return@withSpan orderRepository.coSave(order)
        }
    }

    suspend fun openBid(activity: FlowNftOrderActivityBid, item: Item?): Order {
        return withSpan("openBid", "db") {
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

            orderRepository.coSave(order)
        }
    }

    //TODO tests
    suspend fun close(activity: FlowNftOrderActivitySell): Order {
        return withSpan("closeOrder", "db") {
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
                platform = if (activity.payments.any { it.type == PaymentType.SELLER_FEE }) {
                    FlowOrderPlatformDto.RARIBLE
                } else FlowOrderPlatformDto.OTHER
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
                takePriceUsd = activity.priceUsd,
                platform = if (activity.payments.any { it.type == PaymentType.SELLER_FEE }) {
                    FlowOrderPlatformDto.RARIBLE
                } else FlowOrderPlatformDto.OTHER
            )

            orderRepository.coSave(order)
        }
    }

    suspend fun closeBid(activity: FlowNftOrderActivitySell, item: Item?): Order {
        return withSpan("closeBid", "db") {
            val order = orderRepository.coFindById(activity.hash.toLong())?.let { order ->
                order.copy(
                    fill = order.makeStock!!,
                    makeStock = BigDecimal.ZERO,
                    taker = FlowAddress(activity.right.maker),
                    status = OrderStatus.FILLED,
                    data = OrderData(
                        activity.payments.filter {
                            it.type == PaymentType.REWARD
                        }.map { Payout(account = FlowAddress(it.address), value = it.amount) },
                        activity.payments.filter { it.type in arrayOf(PaymentType.SELLER_FEE, PaymentType.BUYER_FEE) }
                            .map { Payout(account = FlowAddress(it.address), value = it.amount) }),
                    lastUpdatedAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
                    platform = if (activity.payments.any { it.type == PaymentType.BUYER_FEE }) {
                        FlowOrderPlatformDto.RARIBLE
                    } else FlowOrderPlatformDto.OTHER
                )
            } ?: Order(
                id = activity.hash.toLong(),
                fill = activity.right.asset.value,
                makeStock = BigDecimal.ZERO,
                taker = FlowAddress(activity.right.maker),
                status = OrderStatus.FILLED,
                createdAt = LocalDateTime.ofInstant(activity.timestamp, ZoneOffset.UTC),
                maker = FlowAddress(activity.left.maker),
                itemId = ItemId(activity.right.asset.contract, activity.tokenId),
                amount = activity.price,
                collection = activity.contract,
                make = activity.right.asset,
                take = activity.left.asset,
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
                type = OrderType.BID,
                takePriceUsd = activity.priceUsd,
                platform = if (activity.payments.any { it.type == PaymentType.BUYER_FEE }) {
                    FlowOrderPlatformDto.RARIBLE
                } else FlowOrderPlatformDto.OTHER
            )

            orderRepository.coSave(order)
        }
    }

    //TODO tests
    suspend fun cancel(activity: FlowNftOrderActivityCancelList, item: Item?): Order {
        return withSpan("cancelList", "db") {
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

            orderRepository.coSave(order)
        }
    }

    suspend fun enrichTransfer(txHash: String, from: String, to: String): ItemHistory? {
        return withSpan("enrichTransfer", "db") {
            itemHistoryRepository.findTransferInTx(txHash, from, to).awaitFirstOrNull()
                ?.let { transfer ->
                    (transfer.activity as? TransferActivity)?.let { transferActivity ->
                        itemHistoryRepository.save(transfer.copy(activity = transferActivity.copy(purchased = true)))
                            .awaitSingle()
                    }
                }
        }
    }

    suspend fun checkAndEnrichTransfer(txHash: String, from: String, to: String) =
        itemHistoryRepository.findOrderInTx(txHash, from, to).awaitFirstOrNull()?.let { sell ->
            (sell.activity as? FlowNftOrderActivitySell)?.let { activity ->
                if (
                    activity.left.maker == from && activity.right.maker == to ||
                    activity.right.maker == from && activity.left.maker == to
                ) enrichTransfer(txHash, from, to)
                else null
            }
        }

    suspend fun enrichCancelList(orderId: String) {
        withSpan("enrichCancelList", "db") {
            val h = itemHistoryRepository
                .findOrderActivity("CANCEL_LIST", orderId).awaitFirstOrNull()
                ?: return@withSpan

            val openActivity = itemHistoryRepository
                .findOrderActivity("LIST", orderId).awaitFirstOrNull()
                ?.let { it.activity as? FlowNftOrderActivityList }
                ?: return@withSpan

            val closeActivity = h.activity as? FlowNftOrderActivityCancelList
                ?: return@withSpan

            val newActivity = closeActivity.copy(
                price = openActivity.price,
                priceUsd = openActivity.priceUsd,
                tokenId = openActivity.tokenId,
                contract = openActivity.contract,
                maker = openActivity.maker,
                make = openActivity.make,
                take = openActivity.take,
            )
            itemHistoryRepository.save(h.copy(activity = newActivity)).awaitSingle()
        }
    }

    suspend fun cancelBid(activity: FlowNftOrderActivityCancelBid, item: Item?): Order {
        return withSpan("cancelBid", "db") {
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

            orderRepository.coSave(order)
        }

    }

    suspend fun enrichCancelBid(orderId: String) {
        withSpan("enrichCancelBid", "db") {
            val h = itemHistoryRepository
                .findOrderActivity("CANCEL_BID", orderId).awaitFirstOrNull()
                ?: return@withSpan

            val openActivity = itemHistoryRepository
                .findOrderActivity("BID", orderId).awaitFirstOrNull()
                ?.let { it.activity as? FlowNftOrderActivityBid }
                ?: return@withSpan

            val closeActivity = h.activity as? FlowNftOrderActivityCancelBid
                ?: return@withSpan

            val newActivity = closeActivity.copy(
                price = openActivity.price,
                priceUsd = openActivity.priceUsd,
                tokenId = openActivity.tokenId,
                contract = openActivity.contract,
                maker = openActivity.maker,
                make = openActivity.make,
                take = openActivity.take,
            )
            itemHistoryRepository.save(h.copy(activity = newActivity)).awaitSingle()
        }
    }

    suspend fun deactivateOrdersByOwnership(
        ownership: Ownership,
        before: Instant,
        needSendToKafka: Boolean,
        marks: FlowEventTimeMarksDto
    ): List<Order> = withSpan("deactivateOrdersByOwnership", "db") {
        orderRepository
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
                if (needSendToKafka) protocolEventPublisher.onOrderUpdate(it, orderConverter, marks)
            }
            .toList()
    }

    suspend fun restoreOrdersForOwnership(
        ownership: Ownership,
        before: Instant,
        needSendToKafka: Boolean,
        marks: FlowEventTimeMarksDto
    ): List<Order> = withSpan("restoreOrdersForOwnership", "db") {
        orderRepository
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
                if (needSendToKafka) protocolEventPublisher.onOrderUpdate(it, orderConverter, marks)
            }
            .toList()
    }

    suspend fun deactivateOrdersByItem(item: Item, before: LocalDateTime): List<Order> {
        return withSpan("deactivateOrdersByItem", "db") {
            orderRepository
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
    }

    suspend fun restoreOrdersForItem(item: Item, before: LocalDateTime): List<Order> =
        withSpan("restoreOrdersForItem", "db") {
            orderRepository
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
        }

    suspend fun updateOrdersPrices() {
        withSpan("updateOrdersPrices", "db") {
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
