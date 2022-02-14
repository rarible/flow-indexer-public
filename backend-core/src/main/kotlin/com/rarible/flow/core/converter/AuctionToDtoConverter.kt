package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.AuctionLot
import com.rarible.flow.core.domain.EnglishAuctionLot
import com.rarible.protocol.dto.EnglishV1FlowAuctionDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAuctionDto
import com.rarible.protocol.dto.PayInfoDto
import org.springframework.core.convert.converter.Converter
import java.math.BigDecimal
import com.rarible.flow.core.converter.AuctionStatusToDtoConverter.convert as convertStatus
import com.rarible.flow.core.converter.FlowAssetConverter.convert as convertAsset

object AuctionToDtoConverter : Converter<AuctionLot, FlowAuctionDto> {
    override fun convert(source: AuctionLot): FlowAuctionDto {
        return when (source) {
            is EnglishAuctionLot -> {
                EnglishV1FlowAuctionDto(
                    id = source.id,
                    seller = source.seller.formatted,
                    sell = convertAsset(source.sell),
                    buy = FlowAssetFungibleDto(
                        contract = source.currency,
                        value = BigDecimal.ZERO
                    ),
                    minimalPrice = source.startPrice,
                    minimalStep = source.minStep,
                    createdAt = source.createdAt,
                    lastUpdatedAt = source.lastUpdatedAt,
                    status = convertStatus(source.status),
                    buyPrice = source.hammerPrice,
                    buyPriceUsd = source.hammerPriceUsd,
                    startTime = source.startAt,
                    buyoutPrice = source.buyoutPrice,
                    payouts = source.payments.map {
                        PayInfoDto(
                            account = it.account.formatted,
                            value = it.value
                        )
                    },
                    originalFees = source.originFees.map {
                        PayInfoDto(
                            account = it.account.formatted,
                            value = it.value
                        )
                    },
                    duration = source.duration,
                    contract = source.contract,
                    ongoing = source.ongoing
                )
            }
        }
    }
}
