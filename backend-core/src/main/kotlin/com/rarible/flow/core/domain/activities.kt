package com.rarible.flow.core.domain

import com.querydsl.core.annotations.QueryEmbeddable
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.math.BigDecimal

@QueryEmbeddable
sealed interface FlowActivity

/**
 * Common activity
 *
 * @property type               activity type
 * @property contract           NFT item contract
 * @property collection         NFT item collection
 * @property tokenId            NFT token ID
 */
@QueryEmbeddable
sealed class BaseActivity : FlowActivity {
    abstract val type: FlowActivityType
    abstract val contract: String
    abstract val collection: String
    abstract val tokenId: TokenId /* = kotlin.Long */
}

/**
 * Base NFT Activity
 *
 * @property owner              NFT owner account address
 * @property value              amount of NFT's (default 1)
 * @property transactionHash    transaction ID (UUID)
 * @property blockHash          block ID (UUID)
 * @property blockNumber        block height
 */
@QueryEmbeddable
sealed class FlowNftActivity : BaseActivity() {
    abstract val owner: String?
    abstract val value: Long
    abstract val transactionHash: String
    abstract val blockHash: String
    abstract val blockNumber: Long
}

/**
 * Order activity
 *
 * @property price      order price
 */
@QueryEmbeddable
sealed class FlowNftOrderActivity : BaseActivity() {
    abstract val price: BigDecimal
}

/**
 * Sell activity
 *
 * @property left               buyer
 * @property right              seller
 * @property transactionHash    transaction hash
 * @property blockHash          block hash
 * @property blockNumber        block height
 */
@QueryEmbeddable
data class FlowNftOrderActivitySell(
    override val type: FlowActivityType = FlowActivityType.SELL,
    override val price: BigDecimal,
    override val collection: String,
    override val tokenId: TokenId,
    override val contract: String,
    val left: OrderActivityMatchSide,
    val right: OrderActivityMatchSide,
    val transactionHash: String,
    val blockHash: String,
    val blockNumber: Long,
) : FlowNftOrderActivity()

/**
 * List activity
 *
 * @property hash           TODO????
 * @property maker          NFT item
 */
@QueryEmbeddable
data class FlowNftOrderActivityList(
    override val type: FlowActivityType = FlowActivityType.LIST,
    override val price: BigDecimal,
    override val collection: String,
    override val tokenId: TokenId,
    override val contract: String,
    val hash: String,
    val maker: String,
    val make: FlowAsset,
    val take: FlowAsset,
) : FlowNftOrderActivity()
@QueryEmbeddable
data class FlowNftOrderActivityCancelList(
    override val type: FlowActivityType = FlowActivityType.CANCEL_LIST,
    override val price: BigDecimal,
    override val collection: String,
    override val tokenId: TokenId,
    override val contract: String,
    val hash: String,
    val maker: String,
    val make: FlowAsset,
    val take: FlowAsset,
) : FlowNftOrderActivity()

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
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long,
    override val collection: String
) : FlowNftActivity()

/**
 * Burn Activity
 */
@QueryEmbeddable
data class BurnActivity(
    override val type: FlowActivityType = FlowActivityType.BURN,
    override val owner: String,
    override val contract: String,
    override val tokenId: TokenId,
    override val value: Long = 1L,
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long,
    override val collection: String
) : FlowNftActivity()

/**
 * Transfer Activity
 * @property from   sender address
 */
@QueryEmbeddable
data class TransferActivity(
    override val type: FlowActivityType = FlowActivityType.TRANSFER,
    override val owner: String,
    override val contract: String,
    override val tokenId: TokenId,
    override val value: Long,
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long,
    override val collection: String,
    val from: String,
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
    @Field(targetType = FieldType.DECIMAL128)
    override val value: BigDecimal,
    val tokenId: TokenId
) : FlowAsset()
@QueryEmbeddable
data class FlowAssetFungible(
    override val contract: String,
    @Field(targetType = FieldType.DECIMAL128)
    override val value: BigDecimal,
) : FlowAsset()
@QueryEmbeddable
data class OrderActivityMatchSide(
    val maker: String,
    val asset: FlowAsset
)


