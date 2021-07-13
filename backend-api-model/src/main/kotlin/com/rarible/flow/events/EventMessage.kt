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

            eventName.contains("regularsaleorder.orderopened", true) ->
                NftEvent.OrderOpened(
                    eventId = eventId,
                    id = nftId.toULong(),
                    askType = fields["askType"] as String,
                    askId = (fields["askId"] as String).toULong(),
                    bidType = fields["bidType"] as String,
                    bidAmount = (fields["bidAmount"] as String).toBigDecimal(),
                    buyerFee = (fields["buyerFee"] as String).toBigDecimal(),
                    sellerFee = (fields["sellerFee"] as String).toBigDecimal(),
                    maker = FlowAddress(fields["maker"] as String)
                )

            eventName.contains("regularsaleorder.orderclosed", true) ->
                NftEvent.OrderClosed(
                    eventId = eventId,
                    id = nftId.toULong(),
                )

            eventName.contains("regularsaleorder.orderwithdraw", true) ->
                NftEvent.OrderWithdraw(
                    eventId = eventId,
                    id = nftId.toULong()
                )


            eventName.contains("mint", true) ->
                NftEvent.Mint(eventId, nftId.toULong(), FlowAddress(fields["to"]!! as String))

            eventName.contains("withdraw", true) ->
                NftEvent.Withdraw(eventId, nftId.toULong(), FlowAddress(fields["from"]!! as String))

            eventName.contains("deposit", true) ->
                NftEvent.Deposit(eventId, nftId.toULong(), FlowAddress(fields["to"]!! as String))

            eventName.contains("destroy", true) ->
                NftEvent.Destroy(eventId, nftId.toULong())

            eventName.contains("list", true) ->
                NftEvent.List(eventId, nftId.toULong())

            eventName.contains("unlist", true) ->
                NftEvent.Unlist(eventId, nftId.toULong())

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
    JsonSubTypes.Type(NftEvent.Destroy::class, name = "destroy"),
    JsonSubTypes.Type(NftEvent.List::class, name = "list"),
    JsonSubTypes.Type(NftEvent.Unlist::class, name = "unlist"),
)
sealed class NftEvent(
    open val eventId: EventId,
    open val id: ULong
) {

    data class Mint(
        override val eventId: EventId, override val id: ULong, val to: FlowAddress
    ) : NftEvent(eventId, id)

    data class Withdraw(
        override val eventId: EventId, override val id: ULong, val from: FlowAddress
    ): NftEvent(eventId, id)

    data class Deposit(
        override val eventId: EventId, override val id: ULong, val to: FlowAddress
    ): NftEvent(eventId, id)

    data class Destroy(
        override val eventId: EventId, override val id: ULong
    ) : NftEvent(eventId, id)

    data class List(
        override val eventId: EventId, override val id: ULong
    ): NftEvent(eventId, id)

    data class Unlist(
        override val eventId: EventId, override val id: ULong
    ): NftEvent(eventId, id)

    /**
     * events for making bids, conforming the structure of com.rarible.flow.core.domain.Order
     */
    data class Bid(
        override val eventId: EventId,
        override val id: ULong,
        val bidder: FlowAddress,
        val amount: BigDecimal
    ): NftEvent(eventId, id)

    data class BidNft(
        override val eventId: EventId,
        override val id: ULong,
        val bidder: FlowAddress,
        val offeredNftAddress: FlowAddress,
        val offeredNftId: Int
    ): NftEvent(eventId, id)

    data class OrderOpened(
        override val eventId: EventId,
        override val id: ULong,
        val askType: String,
        val askId: ULong,
        val bidType: String,
        val bidAmount: BigDecimal,
        val buyerFee: BigDecimal,
        val sellerFee: BigDecimal,
        val maker: FlowAddress
    ): NftEvent(eventId, id)

    data class OrderClosed(
        override val eventId: EventId,
        override val id: ULong,
    ): NftEvent(eventId, id)

    data class OrderWithdraw(
        override val eventId: EventId,
        override val id: ULong,
    ): NftEvent(eventId, id)

    data class OrderAssigned(
        override val eventId: EventId,
        override val id: ULong,
        val to: FlowAddress
    ): NftEvent(eventId, id)
}
