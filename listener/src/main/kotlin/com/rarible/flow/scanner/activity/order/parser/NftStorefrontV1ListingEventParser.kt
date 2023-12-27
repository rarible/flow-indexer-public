package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowId
import com.rarible.flow.core.domain.EstimatedFee
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.service.CurrencyService
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class NftStorefrontV1ListingEventParser(
    currencyService: CurrencyService,
    supportedNftCollectionProvider: SupportedNftCollectionProvider,
    private val chainId: FlowChainId,
    private val txManager: TxManager,
) : AbstractNftStorefrontListingEventParser(currencyService, supportedNftCollectionProvider) {

    private val raribleOrderAddress = mapOf(
        FlowChainId.MAINNET to "01ab36aaf654a13e",
        FlowChainId.TESTNET to "ebf4ae01d1284af8"
    )

    override suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivityList {
        val listActivity = super.parseActivity(logEvent)
        val price = checkRaribleEventPrice(logEvent)
        return if (price != null) listActivity.copy(price = price) else listActivity
    }

    override fun getEstimatedFee(event: EventMessage): EstimatedFee? {
        return null
    }

    override suspend fun getSellPrice(event: EventMessage): BigDecimal {
        return cadenceParser.bigDecimal(event.fields["price"]!!)
    }

    override fun getCurrencyContract(event: EventMessage): String {
        return EventId.of(cadenceParser.type(event.fields["ftVaultType"]!!)).collection()
    }

    override fun getExpiry(blockTimestamp: Instant, event: EventMessage): Instant? {
        return null
    }

    private suspend fun checkRaribleEventPrice(event: FlowLogEvent): BigDecimal? {
        val eventName = "A.${raribleOrderAddress[chainId]}.RaribleOrder.OrderAvailable"
        return txManager.onTransaction(
            blockHeight = event.log.blockHeight,
            transactionId = FlowId(event.log.transactionHash)
        ) { result ->
            val e = result.events.find { it.type == eventName }
            if (e != null) {
                return@onTransaction cadenceParser.bigDecimal(e.event["price"]!!)
            }
            return@onTransaction null
        }
    }
}
