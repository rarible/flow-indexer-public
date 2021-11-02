package com.rarible.flow.scanner.eventlisteners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import java.time.Instant


data class ItemIsWithdrawn(val item: Item, val from: FlowAddress, val activityTime: Instant)

data class ItemIsDeposited(
    val item: Item,
    val to: FlowAddress,
    val from: FlowAddress?,
    val activityTime: Instant
)