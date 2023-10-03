package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import com.rarible.protocol.dto.FlowOrderPlatformDto
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.math.BigDecimal
import java.time.Instant

sealed interface FlowActivity

sealed class TypedFlowActivity : FlowActivity {
    abstract val type: FlowActivityType

    fun isCancelList(): Boolean {
        return this.type == FlowActivityType.CANCEL_LIST
    }
}

sealed class BaseActivity : TypedFlowActivity(), Partitionable {
    abstract val timestamp: Instant
}

sealed class NFTActivity : BaseActivity() {
    abstract val contract: String
    abstract val tokenId: TokenId
    override fun getKey(): String = ItemId(contract, tokenId).toString()
}

sealed class FlowNftActivity : NFTActivity() {
    abstract val owner: String?
    abstract val value: Long
}

sealed class FlowNftOrderActivity : NFTActivity() {
    abstract val price: BigDecimal?
    abstract val priceUsd: BigDecimal?
    abstract val hash: String
    override fun getKey(): String = hash
}

data class FlowNftOrderActivitySell(
    override val type: FlowActivityType = FlowActivityType.SELL,
    @Field(targetType = FieldType.DECIMAL128)
    override val price: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    override val priceUsd: BigDecimal,
    override val timestamp: Instant,
    override val hash: String,
    override val tokenId: TokenId,
    override val contract: String,
    val left: OrderActivityMatchSide,
    val right: OrderActivityMatchSide,
    val payments: List<FlowNftOrderPayment> = emptyList(),
    val estimatedFee: EstimatedFee? = null,
    val platform: FlowOrderPlatformDto? = FlowOrderPlatformDto.RARIBLE
) : FlowNftOrderActivity()

data class FlowNftOrderActivityList(
    override val type: FlowActivityType = FlowActivityType.LIST,
    @Field(targetType = FieldType.DECIMAL128)
    override val price: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    override val priceUsd: BigDecimal,
    override val timestamp: Instant,
    override val hash: String,
    override val tokenId: TokenId,
    override val contract: String,
    val maker: String,
    val make: FlowAsset,
    val take: FlowAsset,
    val estimatedFee: EstimatedFee?,
    val expiry: Instant?,
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
) : BaseActivity() {
    override fun getKey() = hash
}

data class FlowNftOrderActivityBid(
    override val type: FlowActivityType = FlowActivityType.BID,
    override val price: BigDecimal,
    @Field(targetType = FieldType.DECIMAL128)
    override val priceUsd: BigDecimal,
    override val timestamp: Instant,
    override val hash: String,
    override val tokenId: TokenId,
    override val contract: String,
    val maker: String,
    val make: FlowAsset,
    val take: FlowAsset,
) : FlowNftOrderActivity()

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
) : BaseActivity() {
    override fun getKey() = hash
}

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

data class BurnActivity(
    override val type: FlowActivityType = FlowActivityType.BURN,
    override val contract: String,
    override val tokenId: TokenId,
    override val value: Long = 1L,
    override val owner: String? = null,
    override val timestamp: Instant,
) : FlowNftActivity()

enum class FlowActivityType {
    MINT,
    BURN,
    SELL,
    BUY,
    LIST,
    CANCEL_LIST,
    BID,
    MAKE_BID,
    GET_BID,
    CANCEL_BID,
    TRANSFER,
    TRANSFER_FROM,
    TRANSFER_TO,
    LOT_AVAILABLE,
    LOT_COMPLETED,
    LOT_CANCELED,
    LOT_END_TIME_CHANGED,
    LOT_CLEANED,
    OPEN_BID,
    CLOSE_BID,
    INCREASE_BID
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

data class TransferActivity(
    override val type: FlowActivityType = FlowActivityType.TRANSFER,
    override val contract: String,
    override val tokenId: TokenId, /* = kotlin.Long */
    override val timestamp: Instant,
    val from: String,
    val to: String,
    val purchased: Boolean? = false,
) : NFTActivity()

sealed class AuctionActivity : BaseActivity() {
    abstract val lotId: Long
    override fun getKey(): String = lotId.toString()
}

data class AuctionActivityLot(
    override val type: FlowActivityType = FlowActivityType.LOT_AVAILABLE,
    override val timestamp: Instant,
    override val lotId: Long,
    val contract: String,
    val tokenId: TokenId,
    val currency: String,
    val minStep: BigDecimal,
    val startPrice: BigDecimal,
    val buyoutPrice: BigDecimal?,
    val startAt: Instant,
    val duration: Long,
    val finishAt: Instant?,
    val seller: String
) : AuctionActivity()

data class AuctionActivityLotCanceled(
    override val type: FlowActivityType = FlowActivityType.LOT_CANCELED,
    override val timestamp: Instant,
    override val lotId: Long
) : AuctionActivity()

data class AuctionActivityLotHammered(
    override val type: FlowActivityType = FlowActivityType.LOT_COMPLETED,
    override val timestamp: Instant,
    override val lotId: Long,
    val contract: String,
    val tokenId: TokenId,
    val winner: FlowAddress,
    val hammerPrice: BigDecimal,
    val hammerPriceUsd: BigDecimal,
    val payments: List<Payout>,
    val originFees: List<Payout>
) : AuctionActivity()

data class AuctionActivityBidOpened(
    override val type: FlowActivityType = FlowActivityType.OPEN_BID,
    override val timestamp: Instant,
    override val lotId: Long,
    val bidder: String,
    val amount: BigDecimal
) : AuctionActivity()

data class AuctionActivityBidClosed(
    override val type: FlowActivityType = FlowActivityType.CLOSE_BID,
    override val timestamp: Instant,
    override val lotId: Long,
    val bidder: String,
    val isWinner: Boolean
) : AuctionActivity()

data class AuctionActivityLotEndTimeChanged(
    override val type: FlowActivityType = FlowActivityType.LOT_END_TIME_CHANGED,
    override val timestamp: Instant,
    override val lotId: Long,
    val finishAt: Instant
) : AuctionActivity()

data class AuctionActivityLotCleaned(
    override val type: FlowActivityType = FlowActivityType.LOT_CLEANED,
    override val timestamp: Instant,
    override val lotId: Long
) : AuctionActivity()

data class AuctionActivityBidIncreased(
    override val type: FlowActivityType = FlowActivityType.INCREASE_BID,
    override val timestamp: Instant,
    override val lotId: Long,
    val bidder: String,
    val amount: BigDecimal
) : AuctionActivity()
