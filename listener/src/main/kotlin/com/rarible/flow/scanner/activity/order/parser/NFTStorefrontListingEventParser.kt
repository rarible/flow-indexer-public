package com.rarible.flow.scanner.activity.order.parser

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.service.CurrencyService
import java.math.BigDecimal

abstract class NFTStorefrontListingEventParser(
    currencyService: CurrencyService
) : AbstractNFTStorefrontEventParser<FlowNftOrderActivityList>(currencyService) {

    override suspend fun parseActivities(logEvent: List<FlowLogEvent>): Map<FlowLog, FlowNftOrderActivityList> {
        return logEvent
            .filter { it.type == FlowLogType.LISTING_AVAILABLE }
            .associate { it.log to parseActivity(it) }
    }

    open suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivityList {
        val log = logEvent.log
        val event = logEvent.event

        val price = getSellPrice(event)
        val orderId = getOrderId(event)
        val contract = getCurrencyContract(event)

        val rate = usdRate(contract, log.timestamp) ?: BigDecimal.ZERO
        val priceUsd = if (rate > BigDecimal.ZERO) price * rate else BigDecimal.ZERO

        val maker = getMaker(event)
        val nftCollection = getNftCollection(event)
        val tokenId = getTokenId(event)
        val timestamp = log.timestamp

        return FlowNftOrderActivityList(
            price = price,
            priceUsd = priceUsd,
            tokenId = tokenId,
            contract = nftCollection,
            timestamp = timestamp,
            hash = getOrderHash(event),
            maker = maker,
            make = FlowAssetNFT(
                contract = nftCollection,
                value = BigDecimal.ONE,
                tokenId = tokenId
            ),
            take = FlowAssetFungible(
                contract = contract,
                value = price
            )
        )
    }

    protected open suspend fun getSellPrice(event: EventMessage): BigDecimal {
        return cadenceParser.bigDecimal(event.fields["price"]!!)
    }

    protected open fun getCurrencyContract(event: EventMessage): String {
        return EventId.of(cadenceParser.type(event.fields["ftVaultType"]!!)).collection()
    }

    protected open fun getMaker(event: EventMessage): String {
        return cadenceParser.address(event.fields["storefrontAddress"]!!)
    }

    protected open fun getNftCollection(event: EventMessage): String {
        return EventId.of(cadenceParser.type(event.fields["nftType"]!!)).collection()
    }

    protected open fun getTokenId(event: EventMessage): Long {
        return cadenceParser.long(event.fields["nftID"]!!)
    }
}

