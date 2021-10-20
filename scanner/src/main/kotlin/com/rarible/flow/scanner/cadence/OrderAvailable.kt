package com.rarible.flow.scanner.cadence

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.rarible.flow.core.domain.TokenId
import java.math.BigDecimal
import com.nftco.flow.sdk.cadence.unmarshall
import com.rarible.flow.events.EventId

enum class PaymentType {
    BUYER_FEE,
    //TODO
}

data class Payment(
    val type: PaymentType
)

@JsonCadenceConversion(ListingAvailableConverter::class)
data class OrderAvailable(
    val storefrontAddress: FlowAddress,
    val listingResourceID: Long,
    val nftType: EventId,
    val nftID: TokenId,
    val ftVaultType: EventId,
    val price: BigDecimal,
    val offerPrice: BigDecimal,
    val payments: List<Payment>
)

class ListingAvailableConverter: JsonCadenceConverter<OrderAvailable> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): OrderAvailable = unmarshall(value) {
        OrderAvailable(
            FlowAddress(string("storefrontAddress")),
            long("listingResourceID"),
            EventId.of(string("nftType")),
            long("nftID"),
            EventId.of("ftVaultType"),
            BigDecimal(string("price"))
        )
    }
}
