package com.rarible.flow.scanner.model

import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory

data class IndexerEvent(
    val history: ItemHistory,
    val item: Item? = null
) {
    fun activityType(): FlowActivityType = history.activity.type
}