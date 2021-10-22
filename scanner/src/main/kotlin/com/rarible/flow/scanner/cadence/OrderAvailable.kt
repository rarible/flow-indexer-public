package com.rarible.flow.scanner.cadence

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.*
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.events.EventId
import com.rarible.flow.scanner.model.parse
import java.math.BigDecimal

enum class PaymentType {
    BUYER_FEE,
    SELLER_FEE,
    OTHER,
    ROYALTY,
    REWARD,
}

@JsonCadenceConversion(PaymentConverter::class)
data class Payment(
    val type: PaymentType,
    val address: FlowAddress,
    val rate: BigDecimal,
    val amount: BigDecimal,
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

class PaymentConverter : JsonCadenceConverter<Payment> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): Payment = unmarshall(value) {
        Payment(
            PaymentType.valueOf(string("type")),
            FlowAddress(address("address")),
            bigDecimal("rate"),
            bigDecimal("amount"),
        )
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
