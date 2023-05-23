package com.rarible.flow.core.test

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.FlowEventPayload
import com.nftco.flow.sdk.FlowEventResult
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.FlowTransactionResult
import com.nftco.flow.sdk.FlowTransactionStatus
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import kotlinx.coroutines.reactive.publishInternal
import org.apache.activemq.artemis.utils.RandomUtil.randomLong
import org.apache.activemq.artemis.utils.RandomUtil.randomPositiveLong
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

object Data {

    @Deprecated("Use randomItem()")
    fun createItem(): Item {
        return Item(
            "ABC",
            123L,
            FlowAddress("0x01"),
            emptyList(),
            owner = FlowAddress("0x02"),
            mintedAt = Instant.now(),
            collection = "ABC",
            updatedAt = Instant.now()
        )
    }

    fun createOrder(tokenId: Long = randomLong()): Order {
        val itemId = ItemId("0x1a2b3c4d", tokenId)
        val order = Order(
            id = randomPositiveLong(),
            itemId = itemId,
            maker = FlowAddress("0x01"),
            make = FlowAssetNFT(
                contract = itemId.contract,
                value = BigDecimal.valueOf(100L),
                tokenId = itemId.tokenId
            ),
            amount = BigDecimal.valueOf(100L),
//            amountUsd = BigDecimal.valueOf(100L),
            data = OrderData(
                payouts = listOf(Payout(FlowAddress("0x01"), BigDecimal.valueOf(1L))),
                originalFees = listOf(Payout(FlowAddress("0x02"), BigDecimal.valueOf(1L)))
            ),
            collection = "collection",
            take = FlowAssetFungible(
                "FLOW",
                BigDecimal.TEN
            ),
            makeStock = BigDecimal.TEN,
            lastUpdatedAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            type = OrderType.LIST
        )
        return order
    }
}

fun randomItem(
    contract: String = randomString(),
    tokenId: TokenId = randomPositiveLong(),
    creator: FlowAddress = FlowAddress("0x01"),
    royalties: List<Part> = emptyList(),
    owner: FlowAddress? = FlowAddress("0x02"),
    mintedAt: Instant = nowMillis(),
    meta: String? = null,
    collection: String = randomString(),
    updatedAt: Instant = nowMillis()
): Item {
    return Item(
        contract = contract,
        tokenId = tokenId,
        creator = creator,
        royalties = royalties,
        owner = owner,
        mintedAt = mintedAt,
        meta = meta,
        collection = collection,
        updatedAt = updatedAt
    )
}

fun randomOwnership(): Ownership {
    return Ownership(
        contract = randomString(),
        tokenId = randomTokenId(),
        owner = FlowAddress("0x02"),
        creator = FlowAddress("0x02"),
        date = Instant.now(Clock.systemUTC())
    )
}

fun randomTokenId(): TokenId {
    return com.rarible.core.test.data.randomLong()
}

fun randomItemId(): ItemId {
    return ItemId(randomString(), randomTokenId())
}

fun randomOwnershipId(): OwnershipId {
    return OwnershipId(randomString(), randomTokenId(), FlowAddress("0x02"))
}

fun randomMintItemHistory(): ItemHistory {
    return ItemHistory(
        date = Instant.now(),
        activity = randomMintActivity(),
        log = randomFlowLog(),
    )
}

fun randomItemHistory(
    activity: BaseActivity = randomMintActivity(),
    log: FlowLog = randomFlowLog()
): ItemHistory {
    return ItemHistory(
        date = Instant.now(),
        activity = activity,
        log = log,
    )
}

fun ItemHistory.with(
    eventIndex: Int,
    transaction: String
): ItemHistory {
    return this.copy(
        log = log.copy(
            eventIndex = eventIndex,
            transactionHash = transaction
        )
    )
}

fun randomMintActivity(
    contract: String = randomString(),
    tokenId: Long = randomLong()
): MintActivity {
    return MintActivity(
        owner = randomString(),
        contract = contract,
        tokenId = tokenId,
        value = randomLong(),
        timestamp = Instant.now(),
        creator = randomString(),
        royalties = emptyList(),
        metadata = emptyMap(),
    )
}

fun randomTransferActivity(
    contract: String = randomString(),
    tokenId: Long = randomLong()
): TransferActivity {
    return TransferActivity(
        contract = contract,
        tokenId = tokenId,
        from = randomString(),
        to = randomString(),
        timestamp = Instant.now(),
    )
}

fun randomFlowLog(
    blockHash: String = randomString(),
    transactionHash: String = randomString(),
    blockHeight: Long = randomLong(),
    eventIndex: Int = randomInt(),
): FlowLog {
    return FlowLog(
        blockHash = blockHash,
        transactionHash = transactionHash,
        eventIndex = eventIndex,
        blockHeight = blockHeight,
        eventType = randomString(),
        timestamp = Instant.now(),
    )
}

fun randomEventId(): EventId {
    return EventId(
        type = randomString(),
        contractAddress = FlowAddress("0x02"),
        contractName = randomString(),
        eventName = randomString()
    )
}

fun randomEventMessage(): EventMessage {
    return EventMessage(
        eventId = randomEventId(),
        fields =  emptyMap()
    )
}

fun randomFlowLogEvent(
    blockHash: String = randomString(),
    transactionHash: String = randomString(),
    eventIndex: Int = randomInt(),
    event: EventMessage = randomEventMessage()
): FlowLogEvent  {
    return FlowLogEvent(
        log = randomFlowLog(
            blockHash = blockHash,
            transactionHash = transactionHash,
            eventIndex = eventIndex
        ),
        event = event,
        type = FlowLogType.values().random(),
    )
}

fun randomFlowEventResult(
    blockHeight: Long = randomLong(),
    events: List<FlowEvent> = emptyList()
): FlowEventResult {
    return FlowEventResult(
        blockHeight = blockHeight,
        events = events,
        blockId = FlowId("79f853d2775ccf402528603b3178f3e29051f0ee0f9ea55c29bd8c2fbef81905"),
        blockTimestamp = LocalDateTime.now()
    )
}

fun randomFlowTransactionResult(events: List<FlowEvent> = emptyList()): FlowTransactionResult {
    return FlowTransactionResult(
        events = events,
        errorMessage = "",
        status = FlowTransactionStatus.FINALIZED,
        statusCode = 0
    )
}

fun randomFlowEvent(): FlowEvent {
    return FlowEvent(
        type = "Listing",
        transactionId = FlowId("0x"),
        transactionIndex = 0,
        eventIndex = 0,
        payload =  FlowEventPayload(ByteArray(0))
    )
}

