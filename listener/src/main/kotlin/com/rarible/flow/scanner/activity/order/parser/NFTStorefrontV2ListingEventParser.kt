package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class NFTStorefrontV2ListingEventParser(
    currencyApi: CurrencyControllerApi
): NFTStorefrontListingEventParser(currencyApi) {

    override suspend fun getSellPrice(event: EventMessage): BigDecimal {
        return cadenceParser.bigDecimal(event.fields["salePrice"]!!)
    }

    override fun getCurrencyContract(event: EventMessage): String {
        return EventId.of(cadenceParser.type(event.fields["salePaymentVaultType"]!!)).collection()
    }
}

