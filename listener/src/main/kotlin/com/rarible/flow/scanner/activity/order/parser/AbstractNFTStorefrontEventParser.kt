package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.service.CurrencyService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant

abstract class AbstractNFTStorefrontEventParser<T: BaseActivity>(
    private val currencyService: CurrencyService
) : NFTStorefrontEventParser<T> {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()

    protected open fun getOrderHash(event: EventMessage): String {
        return getOrderId(event).toString()
    }

    protected open fun getOrderId(event: EventMessage): Long {
        return cadenceParser.long(event.fields["listingResourceID"]!!)
    }

    protected suspend fun usdRate(contract: String, timestamp: Instant): BigDecimal? {
        return currencyService.getUsdRate(contract, timestamp)
    }

}