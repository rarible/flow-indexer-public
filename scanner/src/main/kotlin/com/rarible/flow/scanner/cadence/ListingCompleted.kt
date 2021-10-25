package com.rarible.flow.scanner.cadence

import com.nftco.flow.sdk.cadence.*

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
