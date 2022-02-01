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
    abstract val timestamp: Instant
}

sealed class NFTActivity: BaseActivity() {
    abstract val contract: String
    abstract val tokenId: TokenId /* = kotlin.Long */

}

/**
 * Base NFT Activity
 *
 * @property owner              NFT owner account address
 * @property value              amount of NFT's (default 1)
 */
sealed class FlowNftActivity : NFTActivity() {
    abstract val owner: String?
    abstract val value: Long
}

/**
 * Order activity
 *
 * @property price      order price
 * @property priceUsd   order price in USD
 */
sealed class FlowNftOrderActivity : NFTActivity() {
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
    val payments: List<FlowNftOrderPayment> = emptyList(),
) : FlowNftOrderActivity()

/**
 * Sell (List) activity
 *
 * @property hash           TODO????
 * @property maker          NFT item
 */
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
) : FlowNftOrderActivity()

data class FlowNftOrderActivityCancelList(
    override val type: FlowActivityType = FlowActivityType.CANCEL_LIST,
    override val timestamp: Instant,
    val hash: String,
    @Field(targetType = FieldType.DECIMAL128)
    val price: BigDecimal? = null,
    @Field(targetType = FieldType.DECIMAL128)
    val priceUsd: BigDecimal? = null,
    val tokenId: TokenId? = null,
    val contract: String? = null,
    val maker: String? = null,
    val make: FlowAsset? = null,
    val take: FlowAsset? = null,
) : BaseActivity()

data class FlowNftOrderActivityBid(
    override val type: FlowActivityType = FlowActivityType.BID,
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
): FlowNftOrderActivity()

data class FlowNftOrderActivityCancelBid(
    override val type: FlowActivityType = FlowActivityType.CANCEL_BID,
    override val timestamp: Instant,
    val hash: String,
    val price: BigDecimal? = null,
    val priceUsd: BigDecimal? = null,
    val tokenId: TokenId? = null,
    val contract: String? = null,
    val maker: String? = null,
    val make: FlowAsset? = null,
    val take: FlowAsset? = null,
): BaseActivity()


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
    val creator: String?,
    val royalties: List<Part>,
    val metadata: Map<String, String>,
    val collection: String? = null
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
     * NFT Sold
     */
    SELL,

    BUY,

    /**
     * List to sell
     */
    LIST,

    /**
     * Cancel listing
     */
    CANCEL_LIST,

    /**
     * Open bid
     */
    BID,
    MAKE_BID,
    GET_BID,

    /**
     * Cancel bid
     */
    CANCEL_BID,

    TRANSFER,
    TRANSFER_FROM,
    TRANSFER_TO,
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

data class TransferActivity(
    override val type: FlowActivityType = FlowActivityType.TRANSFER,
    override val contract: String,
    override val tokenId: TokenId, /* = kotlin.Long */
    override val timestamp: Instant,
    val from: String,
    val to: String
) : NFTActivity()
