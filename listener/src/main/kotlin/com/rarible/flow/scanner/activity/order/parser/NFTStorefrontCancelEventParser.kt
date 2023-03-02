package com.rarible.flow.scanner.activity.order.parser

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.scanner.service.CurrencyService
import org.springframework.stereotype.Component

@Component
class NFTStorefrontCancelEventParser(
    currencyService: CurrencyService
) : NFTStorefrontListingCompletedEventParser<FlowNftOrderActivityCancelList>(currencyService) {

    override suspend fun parseActivities(logEvent: List<FlowLogEvent>): Map<FlowLog, FlowNftOrderActivityCancelList> {
        return logEvent
            .filter { it.type == FlowLogType.LISTING_COMPLETED && !wasPurchased(it.event) }
            .associate { it.log to parseActivity(it) }
    }

    suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivityCancelList {
        return FlowNftOrderActivityCancelList(
            hash = "${getOrderId(logEvent.event)}",
            timestamp = logEvent.log.timestamp
        )
    }
}

