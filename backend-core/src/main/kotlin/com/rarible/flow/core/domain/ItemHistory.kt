package com.rarible.flow.core.domain

import com.rarible.protocol.dto.*
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant

/**
 * NFT Item history (item and order activities)
 * @property id         ID
 * @property date       date of activity
 * @property activity   activity data (see [FlowNftActivity])
 */
@Document
data class ItemHistory(
    @MongoId
    val id: String,
    @Indexed(direction = IndexDirection.DESCENDING)
    val date: Instant,
    val activity: FlowActivity
)

fun FlowActivity.toDto(id: String, date: Instant): FlowActivityDto  =
    when(this) {
        is MintActivity -> MintDto(
            id = id,
            date = date,
            owner = this.owner.formatted,
            contract = this.contract,
            value = this.value.toString(),
            tokenId = this.tokenId.toString(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1,
        )

        is TransferActivity -> TransferDto(
            id = id,
            date = date,
            from = this.from.formatted,
            owner = this.owner.formatted,
            contract = this.contract,
            value = this.value.toString(),
            tokenId = this.tokenId.toString(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1

        )

        is BurnActivity -> BurnDto(
            id = id,
            date = date,
            owner = "",
            contract = this.contract,
            value = this.value.toString(),
            tokenId = this.tokenId.toString(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1,
        )

        is FlowNftOrderActivitySell -> FlowNftOrderActivitySellDto(
            id = id,
            date = date,
            left = OrderActivityMatchSideDto(
                maker = this.left.maker.formatted,
                asset = FlowAssetNFTDto(
                    contract = this.left.asset.contract,
                    value = this.left.asset.value.toString(),
                    tokenId = (this.left.asset as FlowAssetNFT).tokenId.toString()
                )
            ),
            right = OrderActivityMatchSideDto(
                maker = this.right.maker.formatted,
                asset = FlowAssetFungibleDto(
                    contract = this.right.asset.contract,
                    value = this.right.asset.value.toString()
                )
            ),
            price = this.price.toString(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1
        )
        is FlowNftOrderActivityList -> FlowNftOrderActivityListDto(
            id = id,
            date = date,
            hash = this.hash,
            maker = this.maker.formatted,
            make = FlowAssetNFTDto(
                contract = this.make.contract,
                value = this.make.value.toString(),
                tokenId = (this.make as FlowAssetNFT).tokenId.toString()
            ),
            take = FlowAssetFungibleDto(
                contract = this.take.contract,
                value = this.take.value.toString()
            ),
            price = this.price.toString()
        )
        is FlowNftOrderActivityCancelList -> FlowNftOrderActivityCancelListDto(
            id = id,
            date = date,
            hash = this.hash,
            maker = this.maker.formatted,
            make = FlowAssetNFTDto(
                contract = this.make.contract,
                value = this.make.value.toString(),
                tokenId = (this.make as FlowAssetNFT).tokenId.toString()
            ),
            take = FlowAssetFungibleDto(
                contract = this.take.contract,
                value = this.take.value.toString()
            ),
            price = this.price.toString()
        )
    }

