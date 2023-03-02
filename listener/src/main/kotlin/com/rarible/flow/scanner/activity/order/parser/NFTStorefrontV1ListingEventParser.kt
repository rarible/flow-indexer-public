package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowId
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.service.CurrencyService
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class NFTStorefrontV1ListingEventParser(
    currencyService: CurrencyService,
    properties: FlowListenerProperties,
    private val txManager: TxManager,
): NFTStorefrontListingEventParser(currencyService) {

    private val chainId = properties.chainId

    private val raribleOrderAddress = mapOf(
        FlowChainId.MAINNET to "01ab36aaf654a13e",
        FlowChainId.TESTNET to "ebf4ae01d1284af8"
    )

    override suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivityList {
        val listActivity = super.parseActivity(logEvent)
        val price = checkRaribleEventPrice(logEvent)
        return if (price != null) listActivity.copy(price = price) else listActivity
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