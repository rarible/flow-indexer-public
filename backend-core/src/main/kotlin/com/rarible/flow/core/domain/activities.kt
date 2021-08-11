package com.rarible.flow.core.domain

import org.onflow.sdk.FlowAddress
import java.math.BigDecimal

sealed interface FlowActivity

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
sealed class FlowNftActivity: FlowActivity {
    abstract val type: FlowActivityType
    abstract val owner: FlowAddress?
    abstract val contract: FlowAddress
    abstract val tokenId: TokenId
    abstract val value: Long
    abstract val transactionHash: String
    abstract val blockHash: String
    abstract val blockNumber: Long
    abstract val collection: String
}

sealed class FlowNftOrderActivity: FlowActivity {
    abstract val type: FlowActivityType
    abstract val price: BigDecimal
}

data class FlowNftOrderActivitySell(
    override val type: FlowActivityType = FlowActivityType.SELL,
    override val price: BigDecimal,
    val left: OrderActivityMatchSide,
    val right: OrderActivityMatchSide,
    val transactionHash: String,
    val blockHash: String,
    val blockNumber: Long,
): FlowNftOrderActivity()

data class FlowNftOrderActivityList(
    override val type: FlowActivityType = FlowActivityType.CANCEL_LIST,
    override val price: BigDecimal,
    val hash: String,
    val maker: FlowAddress,
    val make: FlowAsset,
    val take: FlowAsset,
): FlowNftOrderActivity()

data class FlowNftOrderActivityCancelList(
    override val type: FlowActivityType = FlowActivityType.LIST,
    override val price: BigDecimal,
    val hash: String,
    val maker: FlowAddress,
    val make: FlowAsset,
    val take: FlowAsset,
): FlowNftOrderActivity()

/**
 * Mint Activity
 */
data class MintActivity(
    override val type: FlowActivityType = FlowActivityType.MINT,
    override val owner: FlowAddress,
    override val contract: FlowAddress,
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
data class BurnActivity(
    override val type: FlowActivityType = FlowActivityType.BURN,
    override val owner: FlowAddress? = null,
    override val contract: FlowAddress,
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
data class TransferActivity(
    override val type: FlowActivityType = FlowActivityType.TRANSFER,
    override val owner: FlowAddress,
    override val contract: FlowAddress,
    override val tokenId: TokenId,
    override val value: Long,
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long,
    override val collection: String,
    val from: FlowAddress,
): FlowNftActivity()



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

    /**
     * NFT transferred
     */
    TRANSFER,

    /**
     * Cancel listing
     */
    CANCEL_LIST
}

sealed class FlowAsset {
    abstract val contract: FlowAddress
    abstract val value: BigDecimal
}

data class FlowAssetNFT(
    override val contract: FlowAddress,
    override val value: BigDecimal,
    val tokenId: TokenId
): FlowAsset()

data class FlowAssetFungible(
    override val contract: FlowAddress,
    override val value: BigDecimal,
): FlowAsset()

data class OrderActivityMatchSide(val maker: FlowAddress, val asset: FlowAsset)


