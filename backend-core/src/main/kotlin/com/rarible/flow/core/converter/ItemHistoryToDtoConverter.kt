package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.DepositActivity
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.WithdrawnActivity
import com.rarible.flow.log.Log
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.dto.FlowNftOrderActivityListDto
import com.rarible.protocol.dto.FlowNftOrderActivitySellDto
import com.rarible.protocol.dto.FlowOrderActivityMatchSideDto
import org.springframework.core.convert.converter.Converter


object ItemHistoryToDtoConverter: Converter<ItemHistory, FlowActivityDto?> {

    val logger by Log()

    override fun convert(source: ItemHistory): FlowActivityDto? {
        return when(source.activity) {
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

            is FlowNftOrderActivitySell -> FlowNftOrderActivitySellDto(
                id = source.id,
                date = source.date,
                left = FlowOrderActivityMatchSideDto(
                    maker = source.activity.left.maker,
                    asset = FlowAssetNFTDto(
                        contract = source.activity.left.asset.contract,
                        value = source.activity.left.asset.value,
                        tokenId = (source.activity.left.asset as FlowAssetNFT).tokenId.toBigInteger()
                    ),
                    type = FlowOrderActivityMatchSideDto.Type.SELL
                ),
                right = FlowOrderActivityMatchSideDto(
                    maker = source.activity.right.maker,
                    asset = FlowAssetFungibleDto(
                        contract = source.activity.right.asset.contract,
                        value = source.activity.right.asset.value
                    ),
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
                make = FlowAssetNFTDto(
                    contract = source.activity.make.contract,
                    value = source.activity.make.value,
                    tokenId = (source.activity.make as FlowAssetNFT).tokenId.toBigInteger()
                ),
                take = FlowAssetFungibleDto(
                    contract = source.activity.take.contract,
                    value = source.activity.take.value
                ),
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
                maker = source.activity.maker,
                make = FlowAssetNFTDto(
                    contract = source.activity.make.contract,
                    value = source.activity.make.value,
                    tokenId = (source.activity.make as FlowAssetNFT).tokenId.toBigInteger()
                ),
                take = FlowAssetFungibleDto(
                    contract = source.activity.take.contract,
                    value = source.activity.take.value
                ),
                price = source.activity.price,
                transactionHash = source.log.transactionHash,
                blockHash = source.log.blockHash,
                blockNumber = source.log.blockHeight,
                logIndex = source.log.eventIndex,
            )

            is DepositActivity -> null
            is WithdrawnActivity -> null
        }
    }
}