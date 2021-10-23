package com.rarible.flow.scanner.cadence

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.unmarshall

@JsonCadenceConversion(ListingCompletedConverter::class)
data class ListingCompleted(
    val listingResourceID: Long,
    val storefrontResourceID: Long,
    val purchased: Boolean
)

class ListingCompletedConverter: JsonCadenceConverter<ListingCompleted> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): ListingCompleted = unmarshall(value) {
        ListingCompleted(
            long("listingResourceID"),
            long("storefrontResourceID"),
            boolean("purchased")
        )
    }
}
