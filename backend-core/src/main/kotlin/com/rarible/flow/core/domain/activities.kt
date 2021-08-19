package com.rarible.flow.core.domain

import com.querydsl.core.annotations.PropertyType
import com.querydsl.core.annotations.QueryEmbeddable
import com.querydsl.core.annotations.QueryType
import org.onflow.sdk.FlowAddress
import java.math.BigDecimal

@QueryEmbeddable
sealed interface FlowActivity

/**
 * @property tokenId            NFT or Order token ID
 */
@QueryEmbeddable
sealed class BaseActivity: FlowActivity {
    abstract val tokenId: TokenId /* = kotlin.Long */
    abstract val type: FlowActivityType
}

/**
 * Base NFT Activity
 *
 * @property type               activity type
 * @property owner              NFT owner
 * @property contract           NFT source contract
 * @property tokenId            NFT token ID
 * @property value              amount of NFT's (default 1)
 * @property transactionHash    transaction ID (UUID)
 * @property blockHash          block ID (UUID)
 * @property blockNumber        block height
 */
@QueryEmbeddable
sealed class FlowNftActivity: BaseActivity() {
    @get:QueryType(PropertyType.STRING)
    abstract val owner: FlowAddress?
    abstract val contract: String
    abstract val value: Long
    abstract val transactionHash: String
    abstract val blockHash: String
    abstract val blockNumber: Long
    abstract val collection: String
}
@QueryEmbeddable
sealed class FlowNftOrderActivity: BaseActivity() {
    abstract val price: BigDecimal
    abstract val collection: String
}
@QueryEmbeddable
data class FlowNftOrderActivitySell(
    override val type: FlowActivityType = FlowActivityType.SELL,
    override val price: BigDecimal,
    override val collection: String,
    override val tokenId: TokenId,
    val left: OrderActivityMatchSide,
    val right: OrderActivityMatchSide,
    val transactionHash: String,
    val blockHash: String,
    val blockNumber: Long,
): FlowNftOrderActivity()
@QueryEmbeddable
data class FlowNftOrderActivityList(
    override val type: FlowActivityType = FlowActivityType.LIST,
    override val price: BigDecimal,
    override val collection: String,
    override val tokenId: TokenId,
    val hash: String,
    @get:QueryType(PropertyType.STRING)
    val maker: FlowAddress,
    val make: FlowAsset,
    val take: FlowAsset,
): FlowNftOrderActivity()
@QueryEmbeddable
data class FlowNftOrderActivityCancelList(
    override val type: FlowActivityType = FlowActivityType.CANCEL_LIST,
    override val price: BigDecimal,
    override val collection: String,
    override val tokenId: TokenId,
    val hash: String,
    @get:QueryType(PropertyType.STRING)
    val maker: FlowAddress,
    val make: FlowAsset,
    val take: FlowAsset,
): FlowNftOrderActivity()

/**
 * Mint Activity
 */
@QueryEmbeddable
data class MintActivity(
    override val type: FlowActivityType = FlowActivityType.MINT,
    @get:QueryType(PropertyType.STRING)
    override val owner: FlowAddress,
    override val contract: String,
    override val tokenId: TokenId,
    override val value: Long = 1L,
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long,
    override val collection: String
): FlowNftActivity()

/**
 * Burn Activity
 */
@QueryEmbeddable
data class BurnActivity(
    override val type: FlowActivityType = FlowActivityType.BURN,
    @get:QueryType(PropertyType.STRING)
    override val owner: FlowAddress? = null,
    override val contract: String,
    override val tokenId: TokenId,
    override val value: Long = 1L,
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long,
    override val collection: String
): FlowNftActivity()

/**
 * Transfer Activity
 * @property from   sender address
 */
@QueryEmbeddable
data class TransferActivity(
    override val type: FlowActivityType = FlowActivityType.TRANSFER,
    @get:QueryType(PropertyType.STRING)
    override val owner: FlowAddress,
    override val contract: String,
    override val tokenId: TokenId,
    override val value: Long,
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long,
    override val collection: String,
    @get:QueryType(PropertyType.STRING)
    val from: FlowAddress,
): FlowNftActivity()



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

    /**
     * NFT transferred
     */
    TRANSFER,

    /**
     * Cancel listing
     */
    CANCEL_LIST
}
@QueryEmbeddable
sealed class FlowAsset {
    abstract val contract: String
    abstract val value: BigDecimal
}
@QueryEmbeddable
data class FlowAssetNFT(
    override val contract: String,
    override val value: BigDecimal,
    val tokenId: TokenId
): FlowAsset()
@QueryEmbeddable
data class FlowAssetFungible(
    override val contract: String,
    override val value: BigDecimal,
): FlowAsset()
@QueryEmbeddable
data class OrderActivityMatchSide(
    @QueryType(PropertyType.STRING)
    val maker: FlowAddress,
    val asset: FlowAsset
)


