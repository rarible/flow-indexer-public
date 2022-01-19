package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.*
import com.rarible.protocol.dto.*
import org.springframework.core.convert.converter.Converter
import java.math.BigDecimal
import java.math.BigInteger


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
            )

            is TransferActivity -> FlowTransferDto(
                id = source.id,
                date = source.date,
                tokenId = source.activity.tokenId.toBigInteger(),
                contract = source.activity.contract,
                from = source.activity.from,
                owner = source.activity.to,
                value = BigInteger.ONE,
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
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
            )
            is FlowNftOrderActivityCancelList -> FlowNftOrderActivityCancelListDto(
                id = source.id,
                date = source.date,
                hash = source.activity.hash,
                maker = source.activity.maker.orEmpty(),
                make = source.activity.make!!.let(::convertAsset),
                take = source.activity.take?.let(::convertAsset),
                price = source.activity.price ?: BigDecimal.ZERO,
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
            )
            is FlowNftOrderActivityCancelBid -> {
                FlowNftOrderActivityCancelBidDto(
                    id = source.id,
                    date = source.date,
                    hash = source.activity.hash,
                    maker = source.activity.maker.orEmpty(),
                    make = source.activity.make!!.let(::convertAsset),
                    take = source.activity.take?.let(::convertAsset),
                    price = source.activity.price ?: BigDecimal.ZERO,
                    transactionHash = source.log.transactionHash,
                    blockHash = source.log.blockHash,
                    blockNumber = source.log.blockHeight,
                    logIndex = source.log.eventIndex,
                )
            }
        }
    }
}
