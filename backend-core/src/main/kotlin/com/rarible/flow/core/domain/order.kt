package com.rarible.flow.core.domain


import com.nftco.flow.sdk.FlowAddress
import com.rarible.protocol.dto.FlowOrderPlatformDto
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

const val ORDER_COLLECTION = "order"



/**
 * Description of an order
 * @property id             - database ID,
 * @property itemId         - nft id (address:tokenId)
 * @property taker          - buyer
 * @property maker          - seller
 * @property amount         - amount in flow
 * @property fill           - TODO add  doc
 * @property cancelled       - order canceled
 * @property collection     - item collection
 */
@Document(collection = ORDER_COLLECTION)
data class Order(
    @MongoId
    val id: Long,
    @Indexed
    val itemId: ItemId,
    @Indexed
    val maker: FlowAddress,
    @Indexed
    val taker: FlowAddress? = null,
    @Indexed
    val make: FlowAsset,
    @Indexed
    val take: FlowAsset,
    @Indexed
    val type: OrderType? = OrderType.LIST,
    @Field(targetType = FieldType.DECIMAL128)
    val amount: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    val fill: BigDecimal = BigDecimal.ZERO,
    val cancelled: Boolean = false,
    val data: OrderData? = null,
    @Indexed
    val createdAt: LocalDateTime,
    @Indexed
    var lastUpdatedAt: LocalDateTime? = null,
    @Indexed
    val collection: String,

    @Field(targetType = FieldType.DECIMAL128)
    val makeStock: BigDecimal? = BigDecimal.ZERO,
    @Indexed
    val status: OrderStatus = OrderStatus.INACTIVE,

    @Field(targetType = FieldType.DECIMAL128)
    val takePriceUsd: BigDecimal? = BigDecimal.ZERO,
    @Indexed
    val platform: FlowOrderPlatformDto? = null
) {

    @Indexed
    val dbUpdatedAt: Instant = Instant.now()

    fun deactivateBid(makeStock: BigDecimal): Order {
        return this.copy(status = OrderStatus.INACTIVE, makeStock = makeStock)
    }

    fun reactivateBid(): Order {
        return this.copy(status = OrderStatus.ACTIVE, makeStock = this.make.value)
    }
}

enum class OrderType {
    LIST, BID
}

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
