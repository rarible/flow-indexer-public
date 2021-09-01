package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.config.ListenerProperties
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.*

@Component(MintListener.ID)
class MintListener(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler {

    override suspend fun handle(eventMessage: EventMessage) {
        val event = CommonNftMint(eventMessage.fields)
        log.info("Handling [$ID] at [${event.collection}.${event.id}] with fields [${eventMessage.fields}]")

        val to = FlowAddress(event.creator)

        val existingEvent = itemRepository.coFindById(ItemId(event.collection, event.id))

        if (existingEvent == null) {
            val item = Item(
                event.collection,
                event.id,
                to,
                getRoyalties(event.royalties),
                to,
                Instant.now(Clock.systemUTC()),
                event.metadata,
                collection = event.collection
            )

            itemRepository.coSave(item).let {
                val result = protocolEventPublisher.onItemUpdate(it)
                EventHandler.log.info("item update message is sent: $result")
            }

            ownershipRepository.coSave(
                Ownership(
                    event.collection,
                    event.id,
                    to,
                    Instant.now(),
                    creators = listOf(Payout(account = item.creator, value = BigDecimal.ONE))
                )
            )

            itemHistoryRepository.coSave(
                ItemHistory(
                    id = UUID.randomUUID().toString(),
                    date = Instant.now(Clock.systemUTC()),
                    activity = MintActivity(
                        owner = to,
                        contract = event.collection,
                        tokenId = event.id,
                        value = 1L,
                        transactionHash = eventMessage.blockInfo.transactionId,
                        blockHash = eventMessage.blockInfo.blockId,
                        blockNumber = eventMessage.blockInfo.blockHeight,
                        collection = item.collection
                    )
                )
            )
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

        class CommonNftMint(
            val fields: Map<String, Any?>
        ) {
            val id: Long by fields
            val collection: String by fields
            val creator: String by fields
            val metadata: String by fields
            val royalties: List<Map<String, String>> by fields
        }
    }
}
