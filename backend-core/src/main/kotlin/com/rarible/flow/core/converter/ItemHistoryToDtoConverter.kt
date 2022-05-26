package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowNftOrderActivityBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowNftOrderActivityBidDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelBidDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.dto.FlowNftOrderActivityListDto
import com.rarible.protocol.dto.FlowNftOrderActivitySellDto
import com.rarible.protocol.dto.FlowOrderActivityMatchSideDto
import com.rarible.protocol.dto.FlowTransferDto
import java.math.BigDecimal
import java.math.BigInteger
import org.springframework.core.convert.converter.Converter


object ItemHistoryToDtoConverter: Converter<ItemHistory, FlowActivityDto?> {

    private fun convertAsset(asset: FlowAsset) = when (asset) {
        is FlowAssetNFT -> FlowAssetNFTDto(
            contract = asset.contract,
            value = asset.value,
            tokenId = asset.tokenId.toBigInteger(),
        )
        is FlowAssetFungible -> FlowAssetFungibleDto(
            contract = asset.contract,
            value = asset.value,
        )
        else -> throw IllegalStateException("Invalid asset: ${asset.javaClass}")
    }

    override fun convert(source: ItemHistory): FlowActivityDto {
        return when (source.activity) {
            is MintActivity -> FlowMintDto(
                id = source.id,
                date = source.date,
                owner = source.activity.owner,
                contract = source.activity.contract,
                value = source.activity.value.toBigInteger(),
                tokenId = source.activity.tokenId.toBigInteger(),
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
                updatedAt = source.updatedAt,
            )

            is BurnActivity -> FlowBurnDto(
                id = source.id,
                date = source.date,
                owner = source.activity.owner.orEmpty(),
                contract = source.activity.contract,
                value = source.activity.value.toBigInteger(),
                tokenId = source.activity.tokenId.toBigInteger(),
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
                updatedAt = source.updatedAt,
            )

            is TransferActivity -> FlowTransferDto(
                id = source.id,
                date = source.date,
                tokenId = source.activity.tokenId.toBigInteger(),
                contract = source.activity.contract,
                from = source.activity.from,
                owner = source.activity.to,
                value = BigInteger.ONE,
                purchased = source.activity.purchased ?: false,
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
                updatedAt = source.updatedAt,
            )
            is FlowNftOrderActivitySell -> FlowNftOrderActivitySellDto(
                id = source.id,
                date = source.date,
                left = FlowOrderActivityMatchSideDto(
                    maker = source.activity.left.maker,
                    asset = convertAsset(source.activity.left.asset),
                    type = FlowOrderActivityMatchSideDto.Type.SELL
                ),
                right = FlowOrderActivityMatchSideDto(
                    maker = source.activity.right.maker,
                    asset = convertAsset(source.activity.right.asset),
                    type = FlowOrderActivityMatchSideDto.Type.BID
                ),
                price = source.activity.price,
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
                platform = source.activity.platform,
                updatedAt = source.updatedAt,
            )
            is FlowNftOrderActivityList -> FlowNftOrderActivityListDto(
                id = source.id,
                date = source.date,
                hash = source.activity.hash,
                maker = source.activity.maker,
                make = convertAsset(source.activity.make),
                take = convertAsset(source.activity.take),
                price = source.activity.price,
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
                updatedAt = source.updatedAt,
            )
            is FlowNftOrderActivityBid -> FlowNftOrderActivityBidDto(
                id = source.id,
                date = source.date,
                hash = source.activity.hash,
                maker = source.activity.maker,
                make = convertAsset(source.activity.make),
                take = convertAsset(source.activity.take),
                price = source.activity.price,
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
                updatedAt = source.updatedAt,
            )
            is FlowNftOrderActivityCancelList -> FlowNftOrderActivityCancelListDto(
                id = source.id,
                date = source.date,
                hash = source.activity.hash,
                maker = source.activity.maker.orEmpty(),
                make = source.activity.make?.let(::convertAsset) ?: FlowAssetNFTDto("", BigDecimal.ZERO, BigInteger.ZERO),
                take = source.activity.take?.let(::convertAsset) ?: FlowAssetFungibleDto("", BigDecimal.ZERO),
                price = source.activity.price ?: BigDecimal.ZERO,
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
                updatedAt = source.updatedAt,
            )
            is FlowNftOrderActivityCancelBid -> {
                FlowNftOrderActivityCancelBidDto(
                    id = source.id,
                    date = source.date,
                    hash = source.activity.hash,
                    maker = source.activity.maker.orEmpty(),
                    make = source.activity.make?.let(::convertAsset) ?: FlowAssetFungibleDto("", BigDecimal.ZERO),
                    take = source.activity.take?.let(::convertAsset) ?: FlowAssetNFTDto("", BigDecimal.ZERO, BigInteger.ZERO),
                    price = source.activity.price ?: BigDecimal.ZERO,
                    transactionHash = source.log.transactionHash,
                    blockHash = source.log.blockHash,
                    blockNumber = source.log.blockHeight,
                    logIndex = source.log.eventIndex,
                    updatedAt = source.updatedAt,
                )
            }
        }

    }
}
