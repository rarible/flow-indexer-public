package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.EstimatedFee
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.service.CurrencyService
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import java.math.BigDecimal
import java.time.Instant

abstract class AbstractNftStorefrontListingEventParser(
    currencyService: CurrencyService,
    supportedNftCollectionProvider: SupportedNftCollectionProvider
) : AbstractNftStorefrontEventParser<FlowNftOrderActivityList>(currencyService, supportedNftCollectionProvider) {

    override fun isSupported(logEvent: FlowLogEvent): Boolean {
        return logEvent.type == FlowLogType.LISTING_AVAILABLE && super.isSupported(logEvent)
    }

    override suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivityList {
        val log = logEvent.log
        val event = logEvent.event

        val price = getSellPrice(event)
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
            ),
            estimatedFee = getEstimatedFee(event),
            expiry = getExpiry(logEvent.log.timestamp, event)
        )
    }

    protected abstract fun getEstimatedFee(event: EventMessage): EstimatedFee?

    protected abstract fun getExpiry(blockTimestamp: Instant, event: EventMessage): Instant?

    protected abstract suspend fun getSellPrice(event: EventMessage): BigDecimal

    internal abstract fun getCurrencyContract(event: EventMessage): String
}
