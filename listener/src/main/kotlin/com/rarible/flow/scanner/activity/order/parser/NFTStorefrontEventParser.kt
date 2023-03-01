package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.event.EventMessage
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal

abstract class NFTStorefrontEventParser<T: BaseActivity>(
    private val currencyApi: CurrencyControllerApi
) {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()

    abstract suspend fun parseActivities(logEvent: List<FlowLogEvent>): Map<FlowLog, T>

    protected open fun getOrderHash(event: EventMessage): String {
        return getOrderId(event).toString()
    }

    protected open fun getOrderId(event: EventMessage): Long {
        return cadenceParser.long(event.fields["listingResourceID"]!!)
    }

    protected suspend fun usdRate(contract: String, timestamp: Long): BigDecimal? = withSpan("usdRate", "network") {
        try {
            withTimeout(10_000L) {
                currencyApi.getCurrencyRate(BlockchainDto.FLOW, contract, timestamp).awaitSingle().rate
            }
        } catch (e: Exception) {
            logger.warn("Unable to fetch USD price rate from currency api: ${e.message}", e)
            null
        }
    }
}

