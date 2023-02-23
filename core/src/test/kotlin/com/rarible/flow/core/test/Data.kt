package com.rarible.flow.core.test

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.flow.core.domain.*
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
