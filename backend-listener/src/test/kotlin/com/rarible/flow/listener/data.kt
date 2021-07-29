package com.rarible.flow.listener

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.TokenId
import org.onflow.sdk.FlowAddress
import java.time.Instant


fun createItem(tokenId: TokenId = 42) = Item(
    FlowAddress("0x01"),
    tokenId,
    FlowAddress("0x01"),
    emptyList(),
    FlowAddress("0x02"),
    Instant.now()
)