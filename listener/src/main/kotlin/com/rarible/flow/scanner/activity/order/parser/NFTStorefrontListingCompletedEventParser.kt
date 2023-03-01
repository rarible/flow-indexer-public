package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.event.EventMessage
import com.rarible.protocol.currency.api.client.CurrencyControllerApi

abstract class NFTStorefrontListingCompletedEventParser<T : BaseActivity>(
    currencyApi: CurrencyControllerApi
) : NFTStorefrontEventParser<T>(currencyApi) {

    protected open fun wasPurchased(event: EventMessage): Boolean {
        return cadenceParser.boolean(event.fields["purchased"]!!)
    }
}