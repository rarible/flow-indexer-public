package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.scanner.service.CurrencyService
import org.springframework.stereotype.Component

@Component
class NFTStorefrontCancelEventParser(
    currencyService: CurrencyService
) : NFTStorefrontListingCompletedEventParser<FlowNftOrderActivityCancelList>(currencyService) {

    override fun isSupported(logEvent: FlowLogEvent): Boolean {
        return logEvent.type == FlowLogType.LISTING_COMPLETED && !wasPurchased(logEvent.event)
    }

    override suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivityCancelList {
        return FlowNftOrderActivityCancelList(
            hash = "${getOrderId(logEvent.event)}",
            timestamp = logEvent.log.timestamp
        )
    }
}

