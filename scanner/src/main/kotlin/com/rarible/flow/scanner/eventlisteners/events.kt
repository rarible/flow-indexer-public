package com.rarible.flow.scanner.eventlisteners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.flow.core.domain.Item
import java.time.Instant


data class ItemIsWithdrawn(val item: Item, val from: FlowAddress, val activityTime: Instant, val source: Source)

data class ItemIsDeposited(
    val item: Item,
    val to: FlowAddress,
    val from: FlowAddress?,
    val activityTime: Instant,
    val source: Source
)
