package com.rarible.flow.core.domain


import com.nftco.flow.sdk.FlowAddress
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime

const val ORDER_COLLECTION = "order"



/**
 * Description of an order
 * @property id             - database ID,
 * @property itemId         - nft id (address:tokenId)
 * @property taker          - buyer
 * @property maker          - seller
 * @property amount         - amount in flow
 * @property offeredNftId   - nft id for nft-nft exchange
 * @property fill           - TODO add  doc
 * @property cancelled       - order canceled
 * @property buyerFee       - fee for buyer
 * @property sellerFee      - fee for seller
 * @property collection     - item collection
 */
@Document(collection = ORDER_COLLECTION)
data class Order(
    @MongoId
    val id: Long,
    val itemId: ItemId,
    val maker: FlowAddress,
    val taker: FlowAddress? = null,
    val make: FlowAsset,
    val take: FlowAsset,

    @Field(targetType = FieldType.DECIMAL128)
    val amount: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    val fill: BigDecimal = BigDecimal.ZERO,
    val cancelled: Boolean = false,
    val data: OrderData,

    val createdAt: LocalDateTime,
    var lastUpdatedAt: LocalDateTime? = null,
    val collection: String,
    val makeStock: BigInteger,
    val status: OrderStatus = OrderStatus.INACTIVE,
)

enum class OrderStatus {
    ACTIVE,
    FILLED,
    HISTORICAL,
    INACTIVE,
    CANCELLED
}

data class OrderData(
    val payouts: List<Payout>,
    val originalFees: List<Payout>
)

data class Payout(
    val account: FlowAddress,

    @Field(targetType = FieldType.DECIMAL128)
    val value: BigDecimal
)
