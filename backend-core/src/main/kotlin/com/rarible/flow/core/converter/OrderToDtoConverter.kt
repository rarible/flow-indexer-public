package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.*
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.dto.*
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset


class OrderToDtoConverter(
    private val currencyApi: CurrencyControllerApi
) {
    suspend fun convert(source: Order): FlowOrderDto {
        val priceUsd = currencyApi.getCurrencyRate(
            BlockchainDto.FLOW,
            source.take.contract,
            Instant.now().toEpochMilli()
        ).awaitSingle()

        return FlowOrderDto(
            id = source.id,
            itemId = source.itemId.toString(),
            maker = source.maker.formatted,
            taker = source.taker?.formatted,
            make = convert(source.make),
            take = convert(source.take),
            fill = source.fill,
            cancelled = source.cancelled,
            createdAt = source.createdAt.toInstant(ZoneOffset.UTC),
            lastUpdateAt = source.lastUpdatedAt!!.toInstant(ZoneOffset.UTC),
            amount = source.amount,
            offeredNftId = "", //TODO not needed
            data = convert(source.data),
            priceUsd = priceUsd.rate * source.take.value,
            collection = source.collection,
            makeStock = source.makeStock,
            status = convert(source.status)
        )
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

    fun convert(
        asset: FlowAsset,
    ) = when (asset) {
        is FlowAssetNFT -> FlowAssetNFTDto(
            contract = asset.contract,
            value = asset.value,
            tokenId = asset.tokenId.toBigInteger()
        )
        is FlowAssetFungible -> FlowAssetFungibleDto(
            contract = asset.contract,
            value = asset.value,
        )
        is FlowAssetEmpty -> FlowAssetFungibleDto(
            contract = "",
            value = 0.toBigDecimal()
        )
    }

    fun convert(status: OrderStatus): FlowOrderStatusDto {
        return when(status) {
            OrderStatus.ACTIVE -> FlowOrderStatusDto.ACTIVE
            OrderStatus.FILLED -> FlowOrderStatusDto.FILLED
            OrderStatus.HISTORICAL -> FlowOrderStatusDto.HISTORICAL
            OrderStatus.INACTIVE -> FlowOrderStatusDto.INACTIVE
            OrderStatus.CANCELLED -> FlowOrderStatusDto.CANCELLED
        }
    }
}
