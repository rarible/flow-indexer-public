package com.rarible.flow.core.domain

import com.querydsl.core.annotations.QueryEmbeddable
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.math.BigDecimal
import java.time.Instant

@QueryEmbeddable
sealed interface FlowActivity

@QueryEmbeddable
sealed class TypedFlowActivity : FlowActivity {
    abstract val type: FlowActivityType
}

/**
 * Common activity
 *
 * @property type               activity type
 * @property contract           NFT item contract ("collection")
 * @property tokenId            NFT token ID
 */
@QueryEmbeddable
sealed class BaseActivity : TypedFlowActivity() {
    abstract val contract: String
    abstract val tokenId: TokenId /* = kotlin.Long */
    abstract val timestamp: Instant
}

/**
 * Base NFT Activity
 *
 * @property owner              NFT owner account address
 * @property value              amount of NFT's (default 1)
 */
@QueryEmbeddable
sealed class FlowNftActivity : BaseActivity() {
    abstract val owner: String?
    abstract val value: Long
}

/**
 * Order activity
 *
 * @property price      order price
 * @property priceUsd   order price in USD
 */
@QueryEmbeddable
sealed class FlowNftOrderActivity : BaseActivity() {
    abstract val price: BigDecimal
    abstract val priceUsd: BigDecimal
}

/**
 * Buy activity
 *
 * @property left               buyer
 * @property right              seller
 */
@QueryEmbeddable
data class FlowNftOrderActivitySell(
    override val type: FlowActivityType = FlowActivityType.SELL,
    @Field(targetType = FieldType.DECIMAL128)
    override val price: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    override val priceUsd: BigDecimal,
    override val tokenId: TokenId,
    override val contract: String,
    override val timestamp: Instant,
    val hash: String,
    val left: OrderActivityMatchSide,
    val right: OrderActivityMatchSide,
) : FlowNftOrderActivity()

/**
 * Sell (List) activity
 *
 * @property hash           TODO????
 * @property maker          NFT item
 */
@QueryEmbeddable
data class FlowNftOrderActivityList(
    override val type: FlowActivityType = FlowActivityType.LIST,
    @Field(targetType = FieldType.DECIMAL128)
    override val price: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    override val priceUsd: BigDecimal,
    override val tokenId: TokenId,
    override val contract: String,
    override val timestamp: Instant,
    val hash: String,
    val maker: String,
    val make: FlowAsset,
    val take: FlowAsset,
    val payments: List<FlowNftOrderPayment>,
) : FlowNftOrderActivity()

@QueryEmbeddable
data class FlowNftOrderActivityCancelList(
    override val type: FlowActivityType = FlowActivityType.CANCEL_LIST,
    @Field(targetType = FieldType.DECIMAL128)
    override val price: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    override val priceUsd: BigDecimal,
    override val tokenId: TokenId,
    override val contract: String,
    override val timestamp: Instant,
    val hash: String,
    val maker: String,
    val make: FlowAsset,
    val take: FlowAsset,
) : FlowNftOrderActivity()

@QueryEmbeddable
data class FlowNftOrderPayment(
    val type: PaymentType,
    val address: String,
    val rate: BigDecimal,
    val amount: BigDecimal,
)

@QueryEmbeddable
enum class PaymentType {
    BUYER_FEE,
    SELLER_FEE,
    OTHER,
    ROYALTY,
    REWARD,
}

/**
 * Mint Activity
 */
@QueryEmbeddable
data class MintActivity(
    override val type: FlowActivityType = FlowActivityType.MINT,
    override val owner: String,
    override val contract: String,
    override val tokenId: TokenId,
    override val value: Long = 1L,
    override val timestamp: Instant,
    val royalties: List<Part>,
    val metadata: Map<String, String>,
) : FlowNftActivity()

/**
 * Burn Activity
 */
@QueryEmbeddable
data class BurnActivity(
    override val type: FlowActivityType = FlowActivityType.BURN,
    override val contract: String,
    override val tokenId: TokenId,
    override val value: Long = 1L,
    override val owner: String? = null,
    override val timestamp: Instant,
) : FlowNftActivity()

/**
 * Activity type
 */
@QueryEmbeddable
enum class FlowActivityType {
    /**
     * Mint NFT
     */
    MINT,

    /**
     * Burn NFT
     */
    BURN,

    /**
     * List to sell
     */
    LIST,

    /**
     * NFT Sold
     */
    SELL,

    TRANSFER,

    /**
     * NFT withdrawn
     */
    WITHDRAWN,

    /**
     * NFT deposit
     */
    DEPOSIT,

    /**
     * Cancel listing
     */
    CANCEL_LIST,
    TRANSFER_TO,
    TRANSFER_FROM,
    BUY
}

@QueryEmbeddable
sealed class FlowAsset {
    abstract val contract: String
    abstract val value: BigDecimal
}

@QueryEmbeddable
data class FlowAssetNFT(
    override val contract: String,
    @Field(targetType = FieldType.DECIMAL128)
    override val value: BigDecimal,
    val tokenId: TokenId,
) : FlowAsset()

@QueryEmbeddable
data class FlowAssetFungible(
    override val contract: String,
    @Field(targetType = FieldType.DECIMAL128)
    override val value: BigDecimal,
) : FlowAsset()

@QueryEmbeddable
object FlowAssetEmpty : FlowAsset() {
    override val contract: String = ""
    override val value: BigDecimal = 0.toBigDecimal()
}

@QueryEmbeddable
data class OrderActivityMatchSide(
    val maker: String,
    val asset: FlowAsset,
)

@QueryEmbeddable
data class FlowTokenWithdrawnActivity(
    val from: String?,
    val amount: BigDecimal,
) : FlowActivity

@QueryEmbeddable
data class FlowTokenDepositedActivity(
    val to: String?,
    val amount: BigDecimal,
) : FlowActivity

@QueryEmbeddable
data class WithdrawnActivity(
    override val type: FlowActivityType = FlowActivityType.WITHDRAWN,
    override val contract: String,
    override val tokenId: TokenId, /* = kotlin.Long */
    override val timestamp: Instant,
    val from: String?,
) : BaseActivity()

@QueryEmbeddable
data class DepositActivity(
    override val type: FlowActivityType = FlowActivityType.DEPOSIT,
    override val contract: String,
    override val tokenId: TokenId, /* = kotlin.Long */
    override val timestamp: Instant,
    val to: String?,
) : BaseActivity()

