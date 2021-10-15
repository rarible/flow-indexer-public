package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import org.springframework.stereotype.Component
import java.time.Instant

@Component(MintListener.ID)
class MintListener(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler {

    override suspend fun handle(eventMessage: EventMessage) {
        val event = CommonNftMint(eventMessage.fields)
        val tokenId = event.id.toLong()
        val eventDate =/* eventMessage.timestamp.toInstant(ZoneOffset.UTC)*/ Instant.now()
        log.info("Handling [$ID] at [${event.collection}.$tokenId] with fields [${eventMessage.fields}]")

        val to = FlowAddress(event.creator)
        val contract = EventId.of(event.collection).collection()
        val existingEvent = itemRepository.coFindById(ItemId(event.collection, tokenId))

        if (existingEvent == null) {
            val item = Item(
                contract,
                tokenId,
                to,
                getRoyalties(event.royalties),
                to,
                eventDate,
                event.metadata,
                collection = contract,
                updatedAt = Instant.now()
            )

            itemRepository.coSave(item).let {
                val result = protocolEventPublisher.onItemUpdate(it)
                EventHandler.log.info("item update message is sent: $result")
            }

            ownershipRepository.coSave(
                Ownership(
                    contract,
                    tokenId,
                    to,
                    eventDate
                )
            ).let {
                protocolEventPublisher.onUpdate(it)
            }


/*            itemHistoryRepository.coSave(
                ItemHistory(
                    id = UUID.randomUUID().toString(),
                    date = eventDate,
                    activity = MintActivity(
                        owner = to.formatted,
                        contract = contract,
                        tokenId = tokenId,
                        value = 1L,
                        collection = contract
                    )
                )
            )*/
        }
    }

    private fun getRoyalties(royalties: List<Map<String, String>>): List<Part> {
        return royalties.map { r ->
            Part(FlowAddress(r["address"] as String), r["fee"]?.toDouble() ?: 0.0)
        }
    }

    companion object {
        const val ID = "CommonNFT.Mint"

        val log by Log()

        class CommonNftMint(fields: Map<String, Any?>) {
            val id: String by fields
            val collection: String by fields
            val creator: String by fields
            val metadata: String by fields
            val royalties: List<Map<String, String>> by fields
        }
    }
}
