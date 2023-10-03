package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.core.domain.EstimatedFee
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.service.CurrencyService
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class NftStorefrontV2ListingEventParser(
    currencyService: CurrencyService,
    supportedCollectionService: SupportedNftCollectionProvider,
) : AbstractNftStorefrontListingEventParser(currencyService, supportedCollectionService) {

    override fun getEstimatedFee(event: EventMessage): EstimatedFee? {
        val receivers = cadenceParser.optional(event.fields["commissionReceivers"]!!) {
            arrayValues(it, JsonCadenceParser::address)
        }
        val amount = getCommissionAmount(event)
        return if (receivers.isNullOrEmpty()) null else EstimatedFee(receivers, amount)
    }

    override suspend fun getSellPrice(event: EventMessage): BigDecimal {
        return cadenceParser.bigDecimal(event.fields["salePrice"]!!)
    }

    override fun getCurrencyContract(event: EventMessage): String {
        return EventId.of(cadenceParser.type(event.fields["salePaymentVaultType"]!!)).collection()
    }

    override fun getExpiry(blockTimestamp: Instant, event: EventMessage): Instant? {
        val expiry = cadenceParser.long(event.fields["expiry"]!!)
        val seconds = Instant.ofEpochSecond(expiry)
        val milli = Instant.ofEpochMilli(expiry)
        return if (milli > blockTimestamp) milli else seconds
    }

    internal fun getCommissionAmount(event: EventMessage): BigDecimal {
        return cadenceParser.bigDecimal(event.fields["commissionAmount"]!!)
    }
}
