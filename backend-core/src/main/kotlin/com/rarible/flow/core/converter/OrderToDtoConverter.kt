package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.*
import com.rarible.protocol.dto.*
import org.springframework.core.convert.converter.Converter
import java.time.ZoneOffset

object OrderToDtoConverter: Converter<Order, FlowOrderDto> {
    override fun convert(source: Order): FlowOrderDto =
        FlowOrderDto(
            id = source.id,
            itemId = source.itemId.toString(),
            maker = source.maker.formatted,
            taker = source.taker?.formatted,
            make = convert(source.make)!!,
            take = convert(source.take),
            fill = source.fill,
            cancelled = source.cancelled,
            createdAt = source.createdAt.toInstant(ZoneOffset.UTC),
            lastUpdateAt = source.lastUpdatedAt!!.toInstant(ZoneOffset.UTC),
            amount = source.amount,
            offeredNftId = source.offeredNftId,
            data = convert(source.data),
            amountUsd = 0.toBigDecimal(), //TODO get currencies rate
            collection = source.collection
        )

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
        asset: FlowAsset?
    ) = when (asset) {
        null -> null
        is FlowAssetNFT -> FlowAssetNFTDto(
            contract = asset.contract,
            value = asset.value,
            tokenId = asset.tokenId.toBigInteger()
        )
        is FlowAssetFungible -> FlowAssetFungibleDto(
            contract = asset.contract,
            value = asset.value,
        )
    }
}
