package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.activity.order.NftStorefrontEventParser
import com.rarible.flow.scanner.service.CurrencyService
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant

abstract class AbstractNftStorefrontEventParser<T: BaseActivity>(
    private val currencyService: CurrencyService,
    supportedNftCollectionProvider: SupportedNftCollectionProvider
) : NftStorefrontEventParser<T> {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val supportedNftCollections = supportedNftCollectionProvider.get()

    override suspend fun parseActivities(logEvent: List<FlowLogEvent>): Map<FlowLog, T> {
        return logEvent
            .filter { isSupported(it) }
            .mapNotNull { event -> safeParseActivity(event)?.let { event.log to it } }
            .toMap()
    }

    override fun isSupported(logEvent: FlowLogEvent): Boolean {
        return getNftCollection(logEvent.event) in supportedNftCollections
    }

    override fun getItemId(event: FlowLogEvent): ItemId {
        val tokeId = getTokenId(event.event)
        val collection = getNftCollection(event.event)
        return ItemId(collection, tokeId)
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

    protected open fun getNftCollection(event: EventMessage): String {
        return EventId.of(cadenceParser.type(event.fields["nftType"]!!)).collection()
    }

    protected open fun wasPurchased(event: EventMessage): Boolean {
        return cadenceParser.boolean(event.fields["purchased"]!!)
    }

    protected open fun getMaker(event: EventMessage): String {
        return cadenceParser.address(event.fields["storefrontAddress"]!!)
    }

    protected open fun getTokenId(event: EventMessage): Long {
        return cadenceParser.long(event.fields["nftID"]!!)
    }

    internal suspend fun usdRate(contract: String, timestamp: Instant): BigDecimal? {
        return currencyService.getUsdRate(contract, timestamp)
    }
}