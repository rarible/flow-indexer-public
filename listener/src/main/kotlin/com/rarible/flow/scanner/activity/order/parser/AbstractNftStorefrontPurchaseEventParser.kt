package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.scanner.service.CurrencyService
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider

abstract class AbstractNftStorefrontPurchaseEventParser(
    currencyService: CurrencyService,
    supportedNftCollectionProvider: SupportedNftCollectionProvider
) : AbstractNftStorefrontEventParser<FlowNftOrderActivitySell>(currencyService, supportedNftCollectionProvider) {

    override fun isSupported(logEvent: FlowLogEvent): Boolean {
        return logEvent.type == FlowLogType.LISTING_COMPLETED && wasPurchased(logEvent.event)
    }
}
