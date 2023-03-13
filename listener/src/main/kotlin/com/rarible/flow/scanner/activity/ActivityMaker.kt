package com.rarible.flow.scanner.activity

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.ItemId

interface ActivityMaker {
    fun getItemId(event: FlowLogEvent): ItemId?

    fun isSupportedCollection(collection: String): Boolean

    suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity>
}
