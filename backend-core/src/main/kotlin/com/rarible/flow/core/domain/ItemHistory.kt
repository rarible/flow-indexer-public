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
    val activity: FlowNftActivity
)

fun  FlowNftActivity.toDto(id: String, date: LocalDateTime): FlowNftActivityDto  =
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
    }

