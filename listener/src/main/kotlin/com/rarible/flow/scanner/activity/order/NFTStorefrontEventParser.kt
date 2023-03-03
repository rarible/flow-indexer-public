package com.rarible.flow.scanner.activity.order

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent

interface NFTStorefrontEventParser<T: BaseActivity> {

    fun isSupported(logEvent: FlowLogEvent): Boolean

    suspend fun parseActivities(logEvent: List<FlowLogEvent>): Map<FlowLog, T>
}