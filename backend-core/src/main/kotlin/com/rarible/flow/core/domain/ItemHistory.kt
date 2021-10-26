package com.rarible.flow.core.domain

import com.querydsl.core.annotations.QueryEntity
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
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
@Document("item_history")
@QueryEntity
data class ItemHistory(
    @Indexed(direction = IndexDirection.DESCENDING)
    @Field(targetType = FieldType.DATE_TIME)
    val date: Instant,
    val activity: FlowActivity,
    override val log: FlowLog
): FlowLogRecord<ItemHistory>() {

    @get:MongoId
    val id: String
        get() = "${log.transactionHash}:${log.eventIndex}"

    override fun withLog(log: FlowLog): FlowLogRecord<ItemHistory> = copy(log = log)
}

fun FlowActivity.toDto(history: ItemHistory): FlowActivityDto  =
    when(this) {
        is MintActivity -> FlowMintDto(
            id = history.id,
            date = history.date,
            owner = this.owner,
            contract = this.contract,
            value = this.value.toBigInteger(),
            tokenId = this.tokenId.toBigInteger(),
            transactionHash = history.log.transactionHash,
            blockHash = history.log.blockHash,
            blockNumber = history.log.blockHeight,
            logIndex = history.log.eventIndex,
        )

        is BurnActivity -> FlowBurnDto(
            id = history.id,
            date = history.date,
            owner = this.owner.orEmpty(),
            contract = this.contract,
            value = this.value.toBigInteger(),
            tokenId = this.tokenId.toBigInteger(),
            transactionHash = history.log.transactionHash,
            blockHash = history.log.blockHash,
            blockNumber = history.log.blockHeight,
            logIndex = history.log.eventIndex,
        )

        is FlowNftOrderActivitySell -> FlowNftOrderActivitySellDto(
            id = history.id,
            date = history.date,
            left = FlowOrderActivityMatchSideDto(
                maker = this.left.maker,
                asset = FlowAssetNFTDto(
                    contract = this.left.asset.contract,
                    value = this.left.asset.value,
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
            transactionHash = history.log.transactionHash,
            blockHash = history.log.blockHash,
            blockNumber = history.log.blockHeight,
            logIndex = history.log.eventIndex,
        )
        is FlowNftOrderActivityList -> FlowNftOrderActivityListDto(
            id = history.id,
            date = history.date,
            hash = this.hash,
            maker = this.maker,
            make = FlowAssetNFTDto(
                contract = this.make.contract,
                value = this.make.value,
                tokenId = (this.make as FlowAssetNFT).tokenId.toBigInteger()
            ),
            take = FlowAssetFungibleDto(
                contract = this.take.contract,
                value = this.take.value
            ),
            price = this.price
        )
        is FlowNftOrderActivityCancelList -> FlowNftOrderActivityCancelListDto(
            id = history.id,
            date = history.date,
            hash = this.hash,
            maker = this.maker,
            make = FlowAssetNFTDto(
                contract = this.make.contract,
                value = this.make.value,
                tokenId = (this.make as FlowAssetNFT).tokenId.toBigInteger()
            ),
            take = FlowAssetFungibleDto(
                contract = this.take.contract,
                value = this.take.value
            ),
            price = this.price
        )
        else -> throw IllegalStateException("Unsupported type of activity: [${(this as TypedFlowActivity).type}]")
    }

