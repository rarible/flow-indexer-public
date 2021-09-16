package com.rarible.flow.listener

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.TokenId
import java.time.Clock
import java.time.Instant


fun createItem(tokenId: TokenId = 42) = Item(
    "0x01",
    tokenId,
    FlowAddress("0x01"),
    emptyList(),
    FlowAddress("0x02"),
    Instant.now(Clock.systemUTC()),
    collection = "collection",
    updatedAt = Instant.now()
)
