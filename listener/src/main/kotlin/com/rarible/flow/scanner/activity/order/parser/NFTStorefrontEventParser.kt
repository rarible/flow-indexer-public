package com.rarible.flow.scanner.activity.order.parser

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent

interface NFTStorefrontEventParser<T: BaseActivity> {

    suspend fun parseActivities(logEvent: List<FlowLogEvent>): Map<FlowLog, T>
}