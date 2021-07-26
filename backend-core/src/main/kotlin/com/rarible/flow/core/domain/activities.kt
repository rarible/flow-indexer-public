package com.rarible.flow.core.domain

import org.onflow.sdk.FlowAddress



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
sealed class FlowNftActivity {
    abstract val type: FlowActivityType
    abstract val owner: FlowAddress
    abstract val contract: FlowAddress
    abstract val tokenId: TokenId
    abstract val value: Long
    abstract val transactionHash: String
    abstract val blockHash: String
    abstract val blockNumber: Long
}

/**
 * Mint Activity
 */
data class MintActivity(
    override val type: FlowActivityType = FlowActivityType.MINT,
    override val owner: FlowAddress,
    override val contract: FlowAddress,
    override val tokenId: TokenId,
    override val value: Long,
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long,
): FlowNftActivity()

/**
 * Burn Activity
 */
data class BurnActivity(
    override val type: FlowActivityType = FlowActivityType.BURN,
    override val owner: FlowAddress,
    override val contract: FlowAddress,
    override val tokenId: TokenId,
    override val value: Long,
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long
): FlowNftActivity()

/**
 * Transfer Activity
 * @property from   sender address
 */
data class TransferActivity(
    override val type: FlowActivityType = FlowActivityType.TRANSFER,
    val from: FlowAddress,
    override val owner: FlowAddress,
    override val contract: FlowAddress,
    override val tokenId: TokenId,
    override val value: Long,
    override val transactionHash: String,
    override val blockHash: String,
    override val blockNumber: Long,
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
    TRANSFER
}




