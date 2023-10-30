package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.FlowOrderPlatformDto
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

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
@Document(Order.COLLECTION)
data class Order(
    @MongoId
    val id: String,
    val itemId: ItemId,
    val maker: FlowAddress,
    val taker: FlowAddress? = null,
    val make: FlowAsset,
    val take: FlowAsset,
    val type: OrderType? = OrderType.LIST,

    @Field(targetType = FieldType.DECIMAL128)
    val amount: BigDecimal,

    @Field(targetType = FieldType.DECIMAL128)
    val fill: BigDecimal = BigDecimal.ZERO,

    val cancelled: Boolean = false,
    val data: OrderData? = null,
    val createdAt: LocalDateTime,
    var lastUpdatedAt: LocalDateTime? = null,
    val collection: String,

    @Field(targetType = FieldType.DECIMAL128)
    val makeStock: BigDecimal? = BigDecimal.ZERO,

    val status: OrderStatus = OrderStatus.INACTIVE,

    @Field(targetType = FieldType.DECIMAL128)
    val takePriceUsd: BigDecimal? = BigDecimal.ZERO,

    val platform: FlowOrderPlatformDto? = null,
    val start: Long? = null,
    val end: Long? = null,
    @Version
    val version: Long? = null
) {
    @Indexed
    var dbUpdatedAt: Instant = Instant.now()

    fun deactivateBid(makeStock: BigDecimal): Order {
        return this.copy(status = OrderStatus.INACTIVE, makeStock = makeStock)
    }

    fun reactivateBid(): Order {
        return this.copy(status = OrderStatus.ACTIVE, makeStock = this.make.value)
    }

    fun withUpdatedStatus(updateTime: Instant = nowMillis()): Order {
        return copy(
            status = actualStatus(),
            lastUpdatedAt = LocalDateTime.ofInstant(updateTime, ZoneOffset.UTC)
        )
    }

    fun isEnded() = isEnded(end)

    private fun actualStatus(): OrderStatus {
        return when {
            status == OrderStatus.ACTIVE && !isAlive() -> OrderStatus.CANCELLED
            status == OrderStatus.INACTIVE && isAlive() -> OrderStatus.ACTIVE
            else -> status
        }
    }

    private fun isAlive() = isStarted(start) && !isEnded(end)

    private fun isEnded(end: Long?): Boolean {
        val now = Instant.now().epochSecond
        return end?.let { it in 1 until now } ?: false
    }

    private fun isStarted(start: Long?): Boolean {
        val now = Instant.now().epochSecond
        return start?.let { it <= now } ?: true
    }

    companion object {

        const val COLLECTION = "order"
    }
}

@Document(Order.COLLECTION)
data class LegacyOrder(
    @MongoId
    val id: Long,
    val itemId: ItemId,
    val maker: FlowAddress,
    val taker: FlowAddress? = null,
    val make: FlowAsset,
    val take: FlowAsset,
    val type: OrderType? = OrderType.LIST,

    @Field(targetType = FieldType.DECIMAL128)
    val amount: BigDecimal,

    @Field(targetType = FieldType.DECIMAL128)
    val fill: BigDecimal = BigDecimal.ZERO,

    val cancelled: Boolean = false,
    val data: OrderData? = null,
    val createdAt: LocalDateTime,
    var lastUpdatedAt: LocalDateTime? = null,
    val collection: String,

    @Field(targetType = FieldType.DECIMAL128)
    val makeStock: BigDecimal? = BigDecimal.ZERO,

    val status: OrderStatus = OrderStatus.INACTIVE,

    @Field(targetType = FieldType.DECIMAL128)
    val takePriceUsd: BigDecimal? = BigDecimal.ZERO,

    val platform: FlowOrderPlatformDto? = null,
    val start: Long? = null,
    val end: Long? = null,
    @Version
    val version: Long? = null
) {
    @Indexed
    var dbUpdatedAt: Instant = Instant.now()

    fun deactivateBid(makeStock: BigDecimal): LegacyOrder {
        return this.copy(status = OrderStatus.INACTIVE, makeStock = makeStock)
    }

    fun reactivateBid(): LegacyOrder {
        return this.copy(status = OrderStatus.ACTIVE, makeStock = this.make.value)
    }

    fun withUpdatedStatus(updateTime: Instant = nowMillis()): LegacyOrder {
        return copy(
            status = actualStatus(),
            lastUpdatedAt = LocalDateTime.ofInstant(updateTime, ZoneOffset.UTC)
        )
    }

    fun isEnded() = isEnded(end)

    private fun actualStatus(): OrderStatus {
        return when {
            status == OrderStatus.ACTIVE && !isAlive() -> OrderStatus.CANCELLED
            status == OrderStatus.INACTIVE && isAlive() -> OrderStatus.ACTIVE
            else -> status
        }
    }

    private fun isAlive() = isStarted(start) && !isEnded(end)

    private fun isEnded(end: Long?): Boolean {
        val now = Instant.now().epochSecond
        return end?.let { it in 1 until now } ?: false
    }

    private fun isStarted(start: Long?): Boolean {
        val now = Instant.now().epochSecond
        return start?.let { it <= now } ?: true
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
) {
    companion object {
        fun withOriginalFees(value: List<Payout>): OrderData {
            return OrderData(
                originalFees = value, payouts = emptyList()
            )
        }
    }
}

data class Payout(
    val account: FlowAddress,
    @Field(targetType = FieldType.DECIMAL128)
    val value: BigDecimal
) {
    companion object {
        val MULTIPLIER: BigDecimal = BigDecimal("10000")
    }
}
