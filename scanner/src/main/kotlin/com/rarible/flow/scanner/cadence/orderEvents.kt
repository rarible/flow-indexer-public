package com.rarible.flow.scanner.cadence

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.*
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.events.EventId
import com.rarible.flow.scanner.model.parse
import java.math.BigDecimal

@JsonCadenceConversion(PaymentConverter::class)
data class Payment(
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
class OrderCancelled(
    val orderAddress: String,
    val orderId: Long,
    val nftType: EventId,
    val nftId: TokenId,
    val vaultType: EventId,
    val price: BigDecimal,
    val cuts: List<PaymentPart>,
)

class PaymentConverter : JsonCadenceConverter<Payment> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): Payment = unmarshall(value) {
        Payment(
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
            EventId.of(string("nftType")),
            long("nftId"),
            EventId.of(string("vaultType")),
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
            EventId.of(string("nftType")),
            long("nftId"),
            EventId.of(string("vaultType")),
            bigDecimal("price"),
            address("orderAddress"),
            arrayValues("cuts") { it.parse() }
        )
    }
}

class OrderCancelledConverter : JsonCadenceConverter<OrderCancelled> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): OrderCancelled = unmarshall(value) {
        OrderCancelled(
            address("orderAddress"),
            long("orderId"),
            EventId.of(string("nftType")),
            long("nftId"),
            EventId.of(string("vaultType")),
            bigDecimal("price"),
            arrayValues("cuts") { it.parse() }
        )
    }
}
