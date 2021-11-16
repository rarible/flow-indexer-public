package com.rarible.flow.scanner.eventlisteners

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipFilter
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.scanner.model.IndexerEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ItemIndexerEventProcessor(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
): IndexerEventsProcessor {

    private val objectMapper = ObjectMapper()

    private val supportedTypes = arrayOf(FlowActivityType.MINT, FlowActivityType.TRANSFER, FlowActivityType.BURN)

    override fun isSupported(event: IndexerEvent): Boolean = event.activityType() in supportedTypes

    override suspend fun process(event: IndexerEvent) {
        when(event.activityType()) {
            FlowActivityType.MINT -> mintItemEvent(event)
            FlowActivityType.TRANSFER -> transfer(event)
            FlowActivityType.BURN -> burnItemEvent(event)
            else -> throw IllegalStateException("Unsupported item event!  [${event.activityType()}]")
        }
    }

    suspend fun mintItemEvent(event: IndexerEvent) {
        val mintActivity = event.history.first().activity as MintActivity
        val depositActivity = event.history.last().activity as DepositActivity
        withSpan("mintItemEvent", type = "event", labels = listOf("itemId" to "${mintActivity.contract}:${mintActivity.tokenId}")) {
            val owner = FlowAddress(depositActivity.to!!)
            val creator = FlowAddress(mintActivity.owner)
            val saved = if (event.item == null) {
                itemRepository.insert(
                    Item(
                        contract = mintActivity.contract,
                        tokenId = mintActivity.tokenId,
                        creator = owner,
                        royalties = mintActivity.royalties,
                        owner = owner,
                        mintedAt = mintActivity.timestamp,
                        meta = objectMapper.writeValueAsString(mintActivity.metadata),
                        collection = mintActivity.contract,
                        updatedAt = depositActivity.timestamp
                    )
                ).awaitSingle()
            } else if (event.item.mintedAt != mintActivity.timestamp) {
                itemRepository.save(
                    event.item.copy(
                        mintedAt = mintActivity.timestamp,
                        creator = creator
                    )
                ).awaitSingle()
            } else event.item

            if (saved != event.item && event.source != Source.REINDEX) {
                protocolEventPublisher.onItemUpdate(saved)
            }

            if (saved.updatedAt <= depositActivity.timestamp) {
                //we should not have ownership records yet
                val deleted = ownershipRepository.deleteAllByContractAndTokenId(saved.contract, saved.tokenId)
                    .asFlow().toList()
                val o = ownershipRepository.save(
                    Ownership(
                        contract = depositActivity.contract,
                        tokenId = depositActivity.tokenId,
                        owner = owner,
                        creator = creator,
                        date = depositActivity.timestamp
                    )
                ).awaitSingle()
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onDelete(deleted)
                    protocolEventPublisher.onUpdate(o)
                }
            }

        }
    }

    private suspend fun burnItemEvent(event: IndexerEvent) {
        val withdraw = event.history.first().activity as WithdrawnActivity
        val burn = event.history.last().activity as BurnActivity
        val item = event.item
        withSpan("burnItemEvent", type = "event", labels = listOf("itemId" to "${withdraw.contract}:${withdraw.tokenId}")) {
            if (item != null && item.updatedAt <= burn.timestamp) {
                itemRepository.save(item.copy(owner = null, updatedAt = burn.timestamp)).awaitFirstOrNull()
                val ownerships =
                    ownershipRepository.deleteAllByContractAndTokenId(item.contract, item.tokenId).asFlow().toList()
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onItemDelete(item.id)
                    protocolEventPublisher.onDelete(ownerships)
                }
            } else {
                val saved = itemRepository.insert(
                    Item(
                        contract = withdraw.contract,
                        tokenId = withdraw.tokenId,
                        royalties = listOf(),
                        creator = FlowAddress(withdraw.from!!),
                        owner = null,
                        mintedAt = Instant.now(),
                        collection = withdraw.contract,
                        updatedAt = burn.timestamp
                    )
                ).awaitSingle()
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onItemDelete(saved.id)
                }
            }
        }
    }

    private suspend fun transfer(event: IndexerEvent) {
        val withdraw = event.history.first().activity as WithdrawnActivity
        val deposit = event.history.last().activity as DepositActivity

        val item = event.item
        val needSendToKafka = event.source != Source.REINDEX
        val prevOwner = FlowAddress(withdraw.from!!)
        val newOwner = FlowAddress(deposit.to!!)

        withSpan("transferItemEvent", type = "event", labels = listOf("itemId" to "${withdraw.contract}:${withdraw.tokenId}")) {
            val prevOwnership = ownershipRepository.findById(OwnershipId(contract = withdraw.contract, tokenId = withdraw.tokenId, owner = prevOwner)).awaitSingleOrNull()
            if (prevOwnership != null) {
                ownershipRepository.delete(prevOwnership).awaitSingleOrNull()
                if (needSendToKafka) {
                    protocolEventPublisher.onDelete(prevOwnership)
                }
            }
            if (item != null && item.updatedAt <= deposit.timestamp && item.owner != null) {
                val newOwnership = ownershipRepository.save(
                    Ownership(
                        contract = deposit.contract,
                        tokenId = deposit.tokenId,
                        owner = newOwner,
                        creator = item.creator,
                        date = deposit.timestamp
                    )
                ).awaitSingle()

                val saved = itemRepository.save(item.copy(owner = newOwner, updatedAt = deposit.timestamp)).awaitSingle()
                if (needSendToKafka) {
                    protocolEventPublisher.onItemUpdate(saved)
                    protocolEventPublisher.onUpdate(newOwnership)
                }
            }
        }

    }

}
