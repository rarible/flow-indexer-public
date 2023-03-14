package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.FlowAssetEmpty
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.util.Log
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.dto.FlowOrderDataDto
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.dto.PayInfoDto
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle


class OrderToDtoConverter(
    private val currencyApi: CurrencyControllerApi
) {

    private val logger by Log()

    suspend fun convert(source: Order): FlowOrderDto {
        try {
            val usdRate = try {
                currencyApi.getCurrencyRate(
                    BlockchainDto.FLOW,
                    source.take.contract,
                    Instant.now().toEpochMilli()
                ).awaitSingle().rate
            } catch (e: Exception) {
                BigDecimal.ZERO
            }

            return FlowOrderDto(
                id = source.id,
                itemId = "${source.itemId}",
                maker = source.maker.formatted,
                taker = null,
                make = FlowAssetConverter.convert(source.make),
                take = FlowAssetConverter.convert(source.take),
                fill = source.fill,
                cancelled = source.cancelled,
                createdAt = source.createdAt.toInstant(ZoneOffset.UTC),
                lastUpdateAt = source.lastUpdatedAt!!.toInstant(ZoneOffset.UTC),
                dbUpdatedAt = source.dbUpdatedAt,
                amount = source.amount,
                data = convert(source.data ?: OrderData(emptyList(), emptyList())),
                priceUsd = usdRate * source.take.value,
                collection = source.collection,
                makeStock = source.makeStock ?: BigDecimal.ZERO,
                status = convert(source.status),
                platform = source.platform,
                start = source.start?.let { Instant.ofEpochSecond(it) },
                end = source.end?.let { Instant.ofEpochSecond(it) }
            )
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Throwable(e)
        }
    }

    fun convert(data: OrderData) = FlowOrderDataDto(
        payouts = data.payouts.map {
            PayInfoDto(
                account = it.account.formatted,
                value = it.value
            )
        },
        originalFees = data.originalFees.map {
            PayInfoDto(
                account = it.account.formatted,
                value = it.value
            )
        }
    )

    fun convert(status: OrderStatus): FlowOrderStatusDto {
        return when(status) {
            OrderStatus.ACTIVE -> FlowOrderStatusDto.ACTIVE
            OrderStatus.FILLED -> FlowOrderStatusDto.FILLED
            OrderStatus.HISTORICAL -> FlowOrderStatusDto.HISTORICAL
            OrderStatus.INACTIVE -> FlowOrderStatusDto.INACTIVE
            OrderStatus.CANCELLED -> FlowOrderStatusDto.CANCELLED
        }
    }

    fun makeStock(order: Order): BigInteger {
        return when(order.make) {
            FlowAssetEmpty -> order.makeStock!!
            is FlowAssetFungible -> order.makeStock!!
            is FlowAssetNFT -> order.makeStock!!
        }.toBigInteger()
    }

    suspend fun page(orders: Flow<Order>, sort: OrderFilter.Sort, size: Int?): FlowOrdersPaginationDto {
        return if(orders.count() == 0) {
            FlowOrdersPaginationDto(emptyList())
        } else {
            FlowOrdersPaginationDto(
                orders.map(this::convert).toList(),
                sort.nextPage(orders, size)
            )
        }
    }
}
