package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigDecimal
import java.time.Instant

sealed interface AuctionLot {
    val type: AuctionType
    val status: AuctionStatus
    val seller: FlowAddress
    val buyer: FlowAddress?
    val sell: FlowAsset
    val currency: String
    val startPrice: BigDecimal
    val minStep: BigDecimal
    val lastBid: Bid?
    val createdAt: Instant
    val lastUpdatedAt: Instant
    val startTime: Instant?
    val endTime: Instant?
    val hammerPrice: BigDecimal?
}

@Document("auction")
data class EnglishAuctionLot(
    override val type: AuctionType = AuctionType.ENGLISH,
    override val status: AuctionStatus,
    override val seller: FlowAddress,
    override val buyer: FlowAddress? = null,
    override val sell: FlowAsset,
    override val currency: String,
    override val lastBid: Bid? = null,
    override val createdAt: Instant,
    override val lastUpdatedAt: Instant,
    override val startTime: Instant? = null,
    override val endTime: Instant? = null,
    override val startPrice: BigDecimal,
    override val minStep: BigDecimal,
    override val hammerPrice: BigDecimal? = null,
    @MongoId(targetType = FieldType.INT64)
    val id: Long,
    val payments: List<Payout> = emptyList(),
    val originFees: List<Payout> = emptyList(),
    val buyoutPrice: BigDecimal? = null,
    val duration: Long? = null,
    val finalized: Boolean = false,
    val hammerPriceUsd: BigDecimal? = null
): AuctionLot

data class Bid(
    val amount: BigDecimal,
    val address: FlowAddress,
    val bidAt: Instant
)

enum class AuctionType {
    ENGLISH
}

enum class AuctionStatus {
    ACTIVE, FINISHED, CANCELED, INACTIVE
}
