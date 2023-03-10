package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.scanner.service.CurrencyService
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import org.springframework.stereotype.Component

@Component
class NftStorefrontCancelEventParser(
    currencyService: CurrencyService,
    supportedNftCollectionProvider: SupportedNftCollectionProvider
) : AbstractNftStorefrontEventParser<FlowNftOrderActivityCancelList>(currencyService, supportedNftCollectionProvider) {

    override fun isSupported(logEvent: FlowLogEvent): Boolean {
        return logEvent.type == FlowLogType.LISTING_COMPLETED &&
               !wasPurchased(logEvent.event) &&
               super.isSupported(logEvent)
    }

    override suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivityCancelList {
        return FlowNftOrderActivityCancelList(
            hash = "${getOrderId(logEvent.event)}",
            timestamp = logEvent.log.timestamp
        )
    }
}

