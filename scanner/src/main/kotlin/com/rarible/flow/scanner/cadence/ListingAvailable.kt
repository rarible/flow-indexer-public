package com.rarible.flow.scanner.cadence

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.rarible.flow.core.domain.TokenId
import java.math.BigDecimal
import com.nftco.flow.sdk.cadence.unmarshall
import com.rarible.flow.events.EventId

@JsonCadenceConversion(ListingAvailableConverter::class)
data class ListingAvailable(
    val storefrontAddress: FlowAddress,
    val listingResourceID: Long,
    val nftType: EventId,
    val nftID: TokenId,
    val ftVaultType: EventId,
    val price: BigDecimal
)

class ListingAvailableConverter: JsonCadenceConverter<ListingAvailable> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): ListingAvailable = unmarshall(value) {
        ListingAvailable(
            FlowAddress(string("storefrontAddress")),
            long("listingResourceID"),
            EventId.of(string("nftType")),
            long("nftID"),
            EventId.of("ftVaultType"),
            BigDecimal(string("price"))
        )
    }
}
