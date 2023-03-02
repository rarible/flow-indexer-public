package com.rarible.flow.scanner.activity.order.parser

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.scanner.service.CurrencyService

abstract class NFTStorefrontPurchaseEventParser(
    currencyService: CurrencyService,
) : NFTStorefrontListingCompletedEventParser<FlowNftOrderActivitySell>(currencyService) {

    override suspend fun parseActivities(logEvent: List<FlowLogEvent>): Map<FlowLog, FlowNftOrderActivitySell> {
        return logEvent
            .filter { it.type == FlowLogType.LISTING_COMPLETED && wasPurchased(it.event) }
            .mapNotNull { event -> parseActivity(event)?.let { event.log to it } }
            .toMap()
    }

    protected abstract suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivitySell?
}