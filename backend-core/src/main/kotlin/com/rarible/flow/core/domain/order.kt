package com.rarible.flow.core.domain


import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Description of an order
 * @property id             - database ID,
 * @property itemId         - nft id (address:tokenId)
 * @property taker          - buyer
 * @property maker          - seller
 * @property amount         - amount in flow
 * @property offeredNftId   - nft id for nft-nft exchange
 * @property fill           - TODO add  doc
 * @property canceled       - order canceled
 * @property buyerFee       - fee for buyer
 * @property sellerFee      - fee for seller
 * @property collection     - item collection
 */
@Document
data class Order(
    @MongoId
    val id: Long,
    val itemId: ItemId,
    val maker: FlowAddress,
    val taker: FlowAddress? = null,
    val make: FlowAsset,
    val take: FlowAsset? = null,
    val amount: BigDecimal,
    val offeredNftId: String? = null,
    val fill: BigDecimal = BigDecimal.ZERO,
    val canceled: Boolean = false,
    val buyerFee: BigDecimal,
    val sellerFee: BigDecimal,
    val data: OrderData,
    val amountUsd: BigDecimal = 0.toBigDecimal(),
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @field:LastModifiedDate
    var lastUpdatedAt: LocalDateTime? = null,
    val collection: String
)

data class OrderData(
    val payouts: List<Payout>,
    val originalFees: List<Payout>
)

data class Payout(
    val account: FlowAddress,
    val value: BigDecimal
)
