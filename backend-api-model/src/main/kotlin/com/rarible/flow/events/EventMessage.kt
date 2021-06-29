package com.rarible.flow.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.onflow.sdk.FlowAddress
import java.math.BigDecimal
import java.time.Instant

data class EventMessage(
    val id: String,
    val fields: Map<String, String>,
    var timestamp: Instant
) {
    companion object {
        fun getTopic(environment: String) =
            "protocol.$environment.flow.indexer.nft.item"
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(NftEvent.Mint::class, name = "mint"),
    JsonSubTypes.Type(NftEvent.Withdraw::class, name = "withdraw"),
    JsonSubTypes.Type(NftEvent.Deposit::class, name = "deposit"),
    JsonSubTypes.Type(NftEvent.Burn::class, name = "burn")
)
sealed class NftEvent(
    open val eventId: EventId,
    open val id: Int
) {

    data class Mint(
        override val eventId: EventId, override val id: Int
    ) : NftEvent(eventId, id)

    data class Withdraw(
        override val eventId: EventId, override val id: Int, val from: FlowAddress
    ): NftEvent(eventId, id)

    data class Deposit(
        override val eventId: EventId, override val id: Int, val to: FlowAddress
    ): NftEvent(eventId, id)

    data class Burn(
        override val eventId: EventId, override val id: Int
    ) : NftEvent(eventId, id)

    data class List(
        override val eventId: EventId, override val id: Int
    ): NftEvent(eventId, id)

    data class Unlist(
        override val eventId: EventId, override val id: Int
    ): NftEvent(eventId, id)

    data class Bid(
        override val eventId: EventId,
        override val id: Int,
        val bidder: FlowAddress,
        val amount: BigDecimal
    ): NftEvent(eventId, id)
}
