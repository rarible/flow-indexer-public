package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.service.CurrencyService

abstract class NFTStorefrontListingCompletedEventParser<T : BaseActivity>(
    currencyService: CurrencyService
) : AbstractNFTStorefrontEventParser<T>(currencyService) {

    protected open fun wasPurchased(event: EventMessage): Boolean {
        return cadenceParser.boolean(event.fields["purchased"]!!)
    }
}