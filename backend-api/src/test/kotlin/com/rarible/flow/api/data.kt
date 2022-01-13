package com.rarible.flow.api

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.OrderType
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.randomAddress
import com.rarible.flow.randomLong
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

fun createOrder(id: Long = randomLong()) = Order(
    id = id,
    itemId = ItemId(FlowAddress("0x01").formatted, 1),
    maker = FlowAddress("0x1000"),
    taker = null,
    make = FlowAssetNFT("0x01", 1.toBigDecimal(), 1),
    take = FlowAssetFungible("FLOW", BigDecimal.TEN),
    amount = BigDecimal.TEN,
    type = OrderType.LIST,
    data = OrderData(emptyList(), emptyList()),
    collection = "ABC",
    fill = 13.37.toBigDecimal(),
    lastUpdatedAt = LocalDateTime.now(),
    createdAt = LocalDateTime.now(),
    makeStock = BigDecimal.TEN
)

object data {
    val CADENCE_NULL = """{"type":"Optional","value":null}"""

    fun randomOwnership(contract: String, tokenId: TokenId) = Ownership(
        contract = contract,
        tokenId = tokenId,
        owner = FlowAddress(randomAddress()),
        date = Instant.now(Clock.systemUTC()),
        creator = FlowAddress(randomAddress())
    )
}