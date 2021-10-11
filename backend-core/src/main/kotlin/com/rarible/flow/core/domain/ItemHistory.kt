package com.rarible.flow.core.domain

import com.querydsl.core.annotations.QueryEntity
import com.rarible.protocol.dto.*
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant

/**
 * NFT Item history (item and order activities)
 * @property id         ID
 * @property date       date of activity
 * @property activity   activity data (see [FlowNftActivity])
 */
@Document
@QueryEntity
data class ItemHistory(
    @MongoId
    val id: String,
    @Indexed(direction = IndexDirection.DESCENDING)
    @Field(targetType = FieldType.DATE_TIME)
    val date: Instant,
    val activity: FlowActivity
)

fun FlowActivity.toDto(id: String, date: Instant): FlowActivityDto  =
    when(this) {
        is MintActivity -> FlowMintDto(
            id = id,
            date = date,
            owner = this.owner,
            contract = this.contract,
            value = this.value.toBigInteger(),
            tokenId = this.tokenId.toBigInteger(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1,
        )

        is TransferActivity -> FlowTransferDto(
            id = id,
            date = date,
            from = this.from,
            owner = this.owner,
            contract = this.contract,
            value = this.value.toBigInteger(),
            tokenId = this.tokenId.toBigInteger(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1

        )

        is BurnActivity -> FlowBurnDto(
            id = id,
            date = date,
            owner = this.owner,
            contract = this.contract,
            value = this.value.toBigInteger(),
            tokenId = this.tokenId.toBigInteger(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1,
        )

        is FlowNftOrderActivitySell -> FlowNftOrderActivitySellDto(
            id = id,
            date = date,
            left = FlowOrderActivityMatchSideDto(
                maker = this.left.maker,
                asset = FlowAssetNFTDto(
                    contract = this.left.asset.contract,
                    value = this.left.asset.value.toBigInteger(),
                    tokenId = (this.left.asset as FlowAssetNFT).tokenId.toBigInteger()
                ),
                type = FlowOrderActivityMatchSideDto.Type.SELL
            ),
            right = FlowOrderActivityMatchSideDto(
                maker = this.right.maker,
                asset = FlowAssetFungibleDto(
                    contract = this.right.asset.contract,
                    value = this.right.asset.value
                ),
                type = FlowOrderActivityMatchSideDto.Type.BID
            ),
            price = this.price,
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1
        )
        is FlowNftOrderActivityList -> FlowNftOrderActivityListDto(
            id = id,
            date = date,
            hash = this.hash,
            maker = this.maker,
            make = FlowAssetNFTDto(
                contract = this.make.contract,
                value = this.make.value.toBigInteger(),
                tokenId = (this.make as FlowAssetNFT).tokenId.toBigInteger()
            ),
            take = FlowAssetFungibleDto(
                contract = this.take.contract,
                value = this.take.value
            ),
            price = this.price
        )
        is FlowNftOrderActivityCancelList -> FlowNftOrderActivityCancelListDto(
            id = id,
            date = date,
            hash = this.hash,
            maker = this.maker,
            make = FlowAssetNFTDto(
                contract = this.make.contract,
                value = this.make.value.toBigInteger(),
                tokenId = (this.make as FlowAssetNFT).tokenId.toBigInteger()
            ),
            take = FlowAssetFungibleDto(
                contract = this.take.contract,
                value = this.take.value
            ),
            price = this.price
        )
    }

