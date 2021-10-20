package com.rarible.flow.scanner.cadence

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.unmarshall

@JsonCadenceConversion(ListingCompletedConverter::class)
data class ListingCompleted(
    val storefrontAddress: FlowAddress,
    val listingResourceID: Long,
    val purchased: Boolean
)

class ListingCompletedConverter: JsonCadenceConverter<ListingCompleted> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): ListingCompleted = unmarshall(value) {
        ListingCompleted(
            FlowAddress(string("storefrontAddress")),
            long("listingResourceID"),
            boolean("purchased")
        )
    }
}
