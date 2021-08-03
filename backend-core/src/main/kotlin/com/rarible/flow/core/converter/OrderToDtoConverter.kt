package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.Order
import com.rarible.protocol.dto.*
import org.springframework.core.convert.converter.Converter
import java.time.ZoneOffset

object OrderToDtoConverter: Converter<Order, FlowOrderDto> {
    override fun convert(source: Order): FlowOrderDto =
        FlowOrderDto(
            itemId = source.itemId.toString(),
            maker = source.maker.formatted,
            taker = source.taker?.formatted,
            make = when (source.make) {
                is FlowAssetNFT -> FlowAssetNFTDto(
                    contract = source.make.contract.formatted,
                    value = "${source.make.value}",
                    tokenId = "${source.itemId.tokenId}"
                )
                is FlowAssetFungible -> FlowAssetFungibleDto(
                    contract = source.make.contract.formatted,
                    value = "${source.make.value}",
                )
            },
            take = when (source.take) {
                null -> null
                is FlowAssetNFT -> FlowAssetNFTDto(
                    contract = source.make.contract.formatted,
                    value = "${source.make.value}",
                    tokenId = "${source.itemId.tokenId}"
                )
                is FlowAssetFungible -> FlowAssetFungibleDto(
                    contract = source.make.contract.formatted,
                    value = "${source.make.value}",
                )

            },
            fill = source.fill.toBigInteger(),
            cancelled = source.canceled,
            createdAt = source.createdAt.toInstant(ZoneOffset.UTC),
            lastUpdateAt = source.lastUpdatedAt!!.toInstant(ZoneOffset.UTC),
            amount = source.amount,
            offeredNftId = source.offeredNftId,
            data = FlowOrderDataDto(
                payouts = source.data.payouts.map { PayInfoDto(
                    account = it.account.formatted,
                    value = "${it.value}"
                ) },
                originalFees = source.data.originalFees.map { PayInfoDto(
                    account = it.account.formatted,
                    value = "${it.value}"
                ) }
            ),
            amountUsd = 0.toBigDecimal() //TODO get currencies course
        )
}
