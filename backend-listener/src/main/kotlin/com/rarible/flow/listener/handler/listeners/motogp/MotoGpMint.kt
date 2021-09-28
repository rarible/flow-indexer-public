package com.rarible.flow.listener.handler.listeners.motogp

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.listener.handler.listeners.SmartContractEventHandler
import com.rarible.flow.log.Log
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@Component(MotoGpMint.ID)
class MotoGpMint(
    private val itemRepository: ItemRepository
) : SmartContractEventHandler {

    override suspend fun handle(eventMessage: EventMessage) {
        val event = Mint(eventMessage.fields)
        val tokenId = event.id.toLong()
        val eventDate = eventMessage.timestamp.toInstant(ZoneOffset.UTC)

        val contract = eventMessage.eventId.collection()
        val existingEvent = itemRepository.coFindById(ItemId(contract, tokenId))

        if (existingEvent == null) {
            val item = Item(
                contract,
                tokenId,
                emptyAddress,
                emptyList(),
                emptyAddress,
                eventDate,
                "",
                collection = contract,
                updatedAt = Instant.now()
            )

            itemRepository.coSave(item)
        }
    }

    companion object {
        const val ID = "CommonNFT.Mint"

        val emptyAddress = FlowAddress("0x0")

        val log by Log()

        class Mint(fields: Map<String, Any?>) {
            val id: String by fields
        }
    }
}
