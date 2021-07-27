package com.rarible.flow.core.domain

import com.rarible.protocol.dto.*
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.LocalDateTime
import java.time.ZoneOffset

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
    val date: LocalDateTime,
    val activity: FlowActivity
)

fun FlowActivity.toDto(id: String, date: LocalDateTime): FlowActivityDto  =
    when(this) {
        is MintActivity -> MintDto(
            id = id,
            date = date.toInstant(ZoneOffset.UTC),
            owner = this.owner.formatted,
            contract = this.contract.formatted,
            value = this.value.toString(),
            tokenId = this.tokenId.toString(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1,
        )

        is TransferActivity -> TransferDto(
            id = id,
            date = date.toInstant(ZoneOffset.UTC),
            from = this.from.formatted,
            owner = this.owner.formatted,
            contract = this.contract.formatted,
            value = this.value.toString(),
            tokenId = this.tokenId.toString(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1

        )

        is BurnActivity -> BurnDto(
            id = id,
            date = date.toInstant(ZoneOffset.UTC),
            owner = "",
            contract = this.contract.formatted,
            value = this.value.toString(),
            tokenId = this.tokenId.toString(),
            transactionHash = this.transactionHash,
            blockHash = this.blockHash,
            blockNumber = this.blockNumber,
            logIndex = -1,
        )

        is FlowNftOrderActivitySell -> FlowNftOrderActivitySellDto(
            id = id,
            date = date.toInstant(ZoneOffset.UTC),
            left = OrderActivityMatchSideDto(
                maker = this.left.maker.formatted,
                asset = FlowAssetNFTDto(
                    contract = this.left.asset.contract.formatted,
                    value = this.left.asset.value.toString(),
                    tokenId = (this.left.asset as FlowAssetNFT).tokenId.toString()
                )
            ),
            right = OrderActivityMatchSideDto(
                maker = this.right.maker.formatted,
                asset = FlowAssetFungibleDto(
                    contract = this.right.asset.contract.formatted,
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
            date = date.toInstant(ZoneOffset.UTC),
            hash = this.hash,
            maker = this.maker.formatted,
            make = FlowAssetNFTDto(
                contract = this.make.contract.formatted,
                value = this.make.value.toString(),
                tokenId = (this.make as FlowAssetNFT).tokenId.toString()
            ),
            take = FlowAssetFungibleDto(
                contract = this.take.contract.formatted,
                value = this.take.value.toString()
            ),
            price = this.price.toString()
        )
    }

