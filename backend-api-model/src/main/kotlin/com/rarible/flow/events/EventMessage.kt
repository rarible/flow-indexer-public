package com.rarible.flow.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.onflow.sdk.FlowAddress
import java.math.BigDecimal
import java.time.LocalDateTime

data class EventMessage(
    val id: String,
    val fields: Map<String, Any?>,
    var timestamp: LocalDateTime
) {
    companion object {
        fun getTopic(environment: String) =
            "protocol.$environment.flow.indexer.nft.item"
    }

    fun convert(): NftEvent? {
        val nftId = fields["id"] as String?
        val eventId = EventId.of(id)
        val eventName = eventId.eventName

        return when {
            nftId == null -> null

            eventName.contains("mint", true) ->
                NftEvent.Mint(eventId, nftId.toInt(), FlowAddress(fields["to"]!! as String))

            eventName.contains("withdraw", true) ->
                NftEvent.Withdraw(eventId, nftId.toInt(), FlowAddress(fields["from"]!! as String))

            eventName.contains("deposit", true) ->
                NftEvent.Withdraw(eventId, nftId.toInt(), FlowAddress(fields["to"]!! as String))

            eventName.contains("burn", true) ->
                NftEvent.Burn(eventId, nftId.toInt())

            eventName.contains("list", true) ->
                NftEvent.List(eventId, nftId.toInt())

            eventName.contains("unlist", true) ->
                NftEvent.Unlist(eventId, nftId.toInt())

            else -> null

        }
    }
}

/**
 * Describes NFT related event
 * eventId - ID of event, e.g. A.1cd85950d20f05b2.NFTProvider.Mint
 * id - ID of NFT
 * to/from - addresses to/from which NFT is moved
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(NftEvent.Mint::class, name = "mint"),
    JsonSubTypes.Type(NftEvent.Withdraw::class, name = "withdraw"),
    JsonSubTypes.Type(NftEvent.Deposit::class, name = "deposit"),
    JsonSubTypes.Type(NftEvent.Burn::class, name = "burn"),
    JsonSubTypes.Type(NftEvent.List::class, name = "list"),
    JsonSubTypes.Type(NftEvent.Unlist::class, name = "unlist"),
)
sealed class NftEvent(
    open val eventId: EventId,
    open val id: Int
) {

    data class Mint(
        override val eventId: EventId, override val id: Int, val to: FlowAddress
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

    //todo document as part of FB-112
    data class Bid(
        override val eventId: EventId,
        override val id: Int,
        val bidder: FlowAddress,
        val amount: BigDecimal
    ): NftEvent(eventId, id)
}
