package com.rarible.flow.core.domain

import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.math.BigDecimal
import java.time.Instant

sealed interface FlowActivity

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
data class FlowNftOrderActivitySell(
    override val type: FlowActivityType = FlowActivityType.SELL,
    override val price: BigDecimal,
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
data class FlowNftOrderActivityList(
    override val type: FlowActivityType = FlowActivityType.LIST,
    override val price: BigDecimal,
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

data class FlowNftOrderActivityCancelList(
    override val type: FlowActivityType = FlowActivityType.CANCEL_LIST,
    override val price: BigDecimal,
    override val priceUsd: BigDecimal,
    override val tokenId: TokenId,
    override val contract: String,
    override val timestamp: Instant,
    val hash: String,
    val maker: String,
    val make: FlowAsset,
    val take: FlowAsset,
) : FlowNftOrderActivity()

data class FlowNftOrderActivityBid(
    override val type: FlowActivityType = FlowActivityType.BID,
    override val price: BigDecimal,
    override val priceUsd: BigDecimal,
    override val tokenId: TokenId,
    override val contract: String,
    override val timestamp: Instant,
    val hash: String,
    val maker: String,
    val make: FlowAsset,
    val take: FlowAsset,
    val payments: List<FlowNftOrderPayment>,
): FlowNftOrderActivity()

data class FlowNftOrderActivityCancelBid(
    override val type: FlowActivityType = FlowActivityType.CANCEL_BID,
    override val price: BigDecimal,
    override val priceUsd: BigDecimal,
    override val tokenId: TokenId,
    override val contract: String,
    override val timestamp: Instant,
    val hash: String,
    val maker: String,
    val make: FlowAsset,
    val take: FlowAsset,
): FlowNftOrderActivity()


data class FlowNftOrderPayment(
    val type: PaymentType,
    val address: String,
    val rate: BigDecimal,
    val amount: BigDecimal,
)

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
    BUY,
    BID,
    CANCEL_BID
}

sealed class FlowAsset {
    abstract val contract: String
    abstract val value: BigDecimal
}

data class FlowAssetNFT(
    override val contract: String,
    @Field(targetType = FieldType.DECIMAL128)
    override val value: BigDecimal,
    val tokenId: TokenId,
) : FlowAsset()

data class FlowAssetFungible(
    override val contract: String,
    @Field(targetType = FieldType.DECIMAL128)
    override val value: BigDecimal,
) : FlowAsset()

object FlowAssetEmpty : FlowAsset() {
    override val contract: String = ""
    override val value: BigDecimal = 0.toBigDecimal()
}

data class OrderActivityMatchSide(
    val maker: String,
    val asset: FlowAsset,
)

data class FlowTokenWithdrawnActivity(
    val from: String?,
    val amount: BigDecimal,
) : FlowActivity

data class FlowTokenDepositedActivity(
    val to: String?,
    val amount: BigDecimal,
) : FlowActivity

data class WithdrawnActivity(
    override val type: FlowActivityType = FlowActivityType.WITHDRAWN,
    override val contract: String,
    override val tokenId: TokenId, /* = kotlin.Long */
    override val timestamp: Instant,
    val from: String?,
) : BaseActivity()

data class DepositActivity(
    override val type: FlowActivityType = FlowActivityType.DEPOSIT,
    override val contract: String,
    override val tokenId: TokenId, /* = kotlin.Long */
    override val timestamp: Instant,
    val to: String?,
) : BaseActivity()

