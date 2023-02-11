package com.rarible.flow.scanner.cadence

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.*
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.model.parse
import java.math.BigDecimal

@JsonCadenceConversion(PaymentConverter::class)
data class Payment(
    val currency: String,
    val type: PaymentType,
    val address: String,
    val rate: BigDecimal,
    val amount: BigDecimal,
)

@JsonCadenceConversion(PaymentPartConverter::class)
data class PaymentPart(
    val address: String,
    val rate: BigDecimal,
)

@JsonCadenceConversion(OrderAvailableConverter::class)
data class OrderAvailable(
    val orderAddress: FlowAddress,
    val orderId: Long,
    val nftType: EventId,
    val nftId: TokenId,
    val vaultType: EventId,
    val price: BigDecimal,
    val offerPrice: BigDecimal,
    val payments: List<Payment>,
)

@JsonCadenceConversion(OrderClosedConverter::class)
data class OrderClosed(
    val orderAddress: String,
    val orderId: Long,
    val nftType: EventId,
    val nftId: TokenId,
    val vaultType: EventId,
    val price: BigDecimal,
    val buyerAddress: String,
    val cuts: List<PaymentPart>,
)

@JsonCadenceConversion(OrderCancelledConverter::class)
data class OrderCancelled(
    val orderAddress: String,
    val orderId: Long,
    val nftType: EventId,
    val nftId: TokenId,
    val vaultType: EventId,
    val price: BigDecimal,
    val cuts: List<PaymentPart>,
)

@JsonCadenceConversion(ListingAvailableConverter::class)
data class ListingAvailable(
    val storefrontAddress: FlowAddress,
    val listingResourceID: Long,
    val nftType: String,
    val nftID: Long,
    val ftVaultType: String,
    val price: BigDecimal
)

@JsonCadenceConversion(ListingCompletedConverter::class)
data class ListingCompleted(
    val listingResourceID: Long,
    val storefrontResourceID: Long,
    val purchased: Boolean
)

@JsonCadenceConversion(ListingDetailsConverter::class)
data class ListingDetails(
    val storefrontID: Long,
    val purchased: Boolean,
    val nftID: Long,
    val nftType: String,
    val salePrice: BigDecimal,
    val saleCuts: List<SaleCut>
)

data class SaleCut(
    val address: FlowAddress,
    val amount: BigDecimal
)

class ListingDetailsConverter: JsonCadenceConverter<ListingDetails> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): ListingDetails = unmarshall(value) {
        ListingDetails(
            storefrontID = long("storefrontID"),
            purchased = boolean("purchased"),
            nftID = long("nftID"),
            nftType = type("nftType"),
            salePrice = bigDecimal("salePrice"),
            saleCuts = arrayValues("saleCuts") {
                SaleCut(
                    address = FlowAddress(compositeValue.getRequiredField<CapabilityField>("receiver").value!!.address),
                    amount = bigDecimal(compositeValue.getRequiredField("amount"))
                )
            }
        )
    }
}

class ListingAvailableConverter: JsonCadenceConverter<ListingAvailable> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): ListingAvailable = unmarshall(value) {
        ListingAvailable(
            storefrontAddress = FlowAddress(address("storefrontAddress")),
            listingResourceID = long("listingResourceID"),
            nftType = type("nftType"),
            nftID = long("nftID"),
            ftVaultType = type("nftType"),
            price = bigDecimal("price")
        )
    }
}

class ListingCompletedConverter: JsonCadenceConverter<ListingCompleted> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): ListingCompleted = unmarshall(value) {
        ListingCompleted(
            long("listingResourceID"),
            long("storefrontResourceID"),
            boolean("purchased")
        )
    }
}

class PaymentConverter : JsonCadenceConverter<Payment> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): Payment = unmarshall(value) {
        Payment(
            "",
            PaymentType.valueOf(string("type")),
            address("address"),
            bigDecimal("rate"),
            bigDecimal("amount"),
        )
    }
}

class PaymentPartConverter : JsonCadenceConverter<PaymentPart> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): PaymentPart = unmarshall(value) {
        PaymentPart(address("address"), bigDecimal("rate"))
    }
}

class OrderAvailableConverter : JsonCadenceConverter<OrderAvailable> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): OrderAvailable = unmarshall(value) {
        OrderAvailable(
            FlowAddress(address("orderAddress")),
            long("orderId"),
            EventId.of(type("nftType")),
            long("nftId"),
            EventId.of(type("vaultType")),
            bigDecimal("price"),
            bigDecimal("offerPrice"),
            arrayValues("payments") { it.parse() }
        )
    }
}

class OrderClosedConverter : JsonCadenceConverter<OrderClosed> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): OrderClosed = unmarshall(value) {
        OrderClosed(
            address("orderAddress"),
            long("orderId"),
            EventId.of(type("nftType")),
            long("nftId"),
            EventId.of(type("vaultType")),
            bigDecimal("price"),
            address("buyerAddress"),
            arrayValues("cuts") { it.parse() }
        )
    }
}

class OrderCancelledConverter : JsonCadenceConverter<OrderCancelled> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): OrderCancelled = unmarshall(value) {
        OrderCancelled(
            address("orderAddress"),
            long("orderId"),
            EventId.of(type("nftType")),
            long("nftId"),
            EventId.of(type("vaultType")),
            bigDecimal("price"),
            arrayValues("cuts") { it.parse() }
        )
    }
}

@JsonCadenceConversion(OpenBidAvailableConverter::class)
data class BidAvailable(
    val bidAddress: FlowAddress,
    val bidId: Long,
    val nftType: EventId,
    val nftId: TokenId,
    val vaultType: EventId,
    val bidPrice: BigDecimal,
    val brutto: BigDecimal,
    val cuts: Map<FlowAddress, BigDecimal>,
)


@JsonCadenceConversion(OpenBidClosedConverter::class)
data class BidCompleted(
    val bidId: Long,
    val purchased: Boolean,
)


class OpenBidAvailableConverter: JsonCadenceConverter<BidAvailable> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): BidAvailable = unmarshall(value) {
        BidAvailable(
            bidAddress = FlowAddress(address("bidAddress")),
            bidId = long("bidId"),
            nftType = EventId.of(type("nftType")),
            nftId = long("nftId"),
            vaultType = EventId.of(type("vaultType")),
            bidPrice = bigDecimal("bidPrice"),
            brutto = bigDecimal("brutto"),
            cuts = dictionaryMap("cuts") { k, v ->
                FlowAddress(address(k)) to bigDecimal(v)
            }
        )
    }
}

class OpenBidClosedConverter: JsonCadenceConverter<BidCompleted> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): BidCompleted = unmarshall(value) {
        BidCompleted(
            bidId = long("bidId"),
            purchased = boolean("purchased"),
        )
    }
}
