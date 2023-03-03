package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
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

    override suspend fun parseActivities(logEvent: List<FlowLogEvent>): Map<FlowLog, T> {
        return logEvent
            .filter { isSupported(it) }
            .mapNotNull { event -> safeParseActivity(event)?.let { event.log to it } }
            .toMap()
    }

    internal open suspend fun safeParseActivity(logEvent: FlowLogEvent): T? {
        return try {
            parseActivity(logEvent)
        } catch (ex: Throwable) {
            val message = buildString {
                append("Can't parser storefront activity: ")
                append("eventType=${logEvent.log.eventType}, ")
                append("height=${logEvent.log.blockHeight}, ")
                append("tx=${logEvent.log.transactionHash}, ")
                append("eventIndex=${logEvent.log.eventIndex}")
            }
            logger.error(message, ex)
            throw ex
        }
    }

    protected abstract suspend fun parseActivity(logEvent: FlowLogEvent): T?

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