package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
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
    val startAt: Instant
    val finishAt: Instant?
    val hammerPrice: BigDecimal?
}

@Document("auction")
data class EnglishAuctionLot(
    @MongoId(targetType = FieldType.INT64)
    val id: Long,
    override val type: AuctionType = AuctionType.ENGLISH,
    @Indexed
    override val status: AuctionStatus,
    @Indexed
    override val seller: FlowAddress,
    @Indexed
    override val buyer: FlowAddress? = null,
    @Indexed
    override val sell: FlowAsset,
    @Indexed
    override val currency: String,
    @Indexed
    override val lastBid: Bid? = null,
    override val createdAt: Instant,
    @Indexed
    override val lastUpdatedAt: Instant,
    @Indexed
    override val startAt: Instant,
    @Indexed
    override val finishAt: Instant? = null,
    @Field(targetType = FieldType.DECIMAL128)
    override val startPrice: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    override val minStep: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    override val hammerPrice: BigDecimal? = null,
    val payments: List<Payout> = emptyList(),
    val originFees: List<Payout> = emptyList(),
    @Field(targetType = FieldType.DECIMAL128)
    val buyoutPrice: BigDecimal? = null,
    @Field(targetType = FieldType.INT64)
    val duration: Long? = null,
    val cleaned: Boolean = false,
    @Field(targetType = FieldType.DECIMAL128)
    val hammerPriceUsd: BigDecimal? = null,
    val ongoing: Boolean = false,
    @Indexed
    val contract: String? = null
) : AuctionLot

data class Bid(
    @Field(targetType = FieldType.DECIMAL128)
    val amount: BigDecimal,
    val address: FlowAddress,
    val bidAt: Instant
)

enum class AuctionType {
    ENGLISH
}

enum class AuctionStatus {
    ACTIVE, FINISHED, CANCELLED, INACTIVE
}
