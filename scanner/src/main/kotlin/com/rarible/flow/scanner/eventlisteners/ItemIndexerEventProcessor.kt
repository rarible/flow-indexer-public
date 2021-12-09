package com.rarible.flow.scanner.eventlisteners

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.events.EventId
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
) : IndexerEventsProcessor {

    private val objectMapper = ObjectMapper()

    private val supportedTypes = arrayOf(FlowActivityType.MINT, FlowActivityType.TRANSFER, FlowActivityType.BURN)

    override fun isSupported(event: IndexerEvent): Boolean = event.activityType() in supportedTypes

    override suspend fun process(event: IndexerEvent) {
        when (event.activityType()) {
            FlowActivityType.MINT -> mintItemEvent(event)
            FlowActivityType.TRANSFER -> transfer(event)
            FlowActivityType.BURN -> burnItemEvent(event)
            else -> throw IllegalStateException("Unsupported item event!  [${event.activityType()}]")
        }
    }

    suspend fun mintItemEvent(event: IndexerEvent) {
        val mintActivity = event.history.activity as MintActivity
        withSpan(
            "mintItemEvent",
            type = "event",
            labels = listOf("itemId" to "${mintActivity.contract}:${mintActivity.tokenId}")
        ) {
            val owner = FlowAddress(mintActivity.owner)
            val creator = FlowAddress(mintActivity.creator)
            val forSave = if (event.item == null) {
                Item(
                    contract = mintActivity.contract,
                    tokenId = mintActivity.tokenId,
                    creator = creator,
                    royalties = mintActivity.royalties,
                    owner = owner,
                    mintedAt = mintActivity.timestamp,
                    meta = objectMapper.writeValueAsString(mintActivity.metadata),
                    collection = mintActivity.contract,
                    updatedAt = mintActivity.timestamp
                )
            } else if (event.item.mintedAt != mintActivity.timestamp) {
                event.item.copy(
                    mintedAt = mintActivity.timestamp,
                    creator = creator
                )

            } else event.item

            if (forSave != event.item) {
                val saved = itemRepository.save(forSave).awaitSingle()
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onItemUpdate(saved)
                }
                if (saved.updatedAt <= mintActivity.timestamp) {
                    //we should not have ownership records yet
                    val deleted = ownershipRepository.deleteAllByContractAndTokenId(forSave.contract, forSave.tokenId)
                        .asFlow().toList()
                    val o = ownershipRepository.save(
                        Ownership(
                            contract = mintActivity.contract,
                            tokenId = mintActivity.tokenId,
                            owner = owner,
                            creator = creator,
                            date = mintActivity.timestamp
                        )
                    ).awaitSingle()
                    if (event.source != Source.REINDEX) {
                        protocolEventPublisher.onDelete(deleted)
                        protocolEventPublisher.onUpdate(o)
                    }
                }
            }
        }
    }

    private suspend fun burnItemEvent(event: IndexerEvent) {
        val burn = event.history.activity as BurnActivity
        val item = event.item
        withSpan("burnItemEvent", type = "event", labels = listOf("itemId" to "${burn.contract}:${burn.tokenId}")) {
            if (item != null && item.updatedAt <= burn.timestamp) {
                itemRepository.save(item.copy(owner = null, updatedAt = burn.timestamp)).awaitFirstOrNull()
                val ownerships =
                    ownershipRepository.deleteAllByContractAndTokenId(item.contract, item.tokenId).asFlow().toList()
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onItemDelete(item.id)
                    protocolEventPublisher.onDelete(ownerships)
                }
            } else if (item == null){
                val saved = itemRepository.insert(
                    Item(
                        contract = burn.contract,
                        tokenId = burn.tokenId,
                        royalties = listOf(),
                        creator = EventId.of(burn.contract).contractAddress,
                        owner = null,
                        mintedAt = Instant.now(),
                        collection = burn.contract,
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
        val transferActivity = event.history.activity as TransferActivity

        val item = event.item
        val needSendToKafka = event.source != Source.REINDEX
        val prevOwner = FlowAddress(transferActivity.from)
        val newOwner = FlowAddress(transferActivity.to)

        val (saved, newOwnership) = withSpan<Pair<Item, Ownership?>>(
            "transferItemEvent",
            type = "event",
            labels = listOf("itemId" to "${transferActivity.contract}:${transferActivity.tokenId}")
        ) {
            val prevOwnership = ownershipRepository.findById(
                OwnershipId(
                    contract = transferActivity.contract,
                    tokenId = transferActivity.tokenId,
                    owner = prevOwner
                )
            ).awaitSingleOrNull()
            if (prevOwnership != null) {
                ownershipRepository.delete(prevOwnership).awaitSingleOrNull()
                if (needSendToKafka) {
                    protocolEventPublisher.onDelete(prevOwnership)
                }
            }
            if (item != null) {
                if (item.updatedAt <= transferActivity.timestamp && item.owner != null) {
                    val newOwnership = ownershipRepository.save(
                        Ownership(
                            contract = transferActivity.contract,
                            tokenId = transferActivity.tokenId,
                            owner = newOwner,
                            creator = item.creator,
                            date = transferActivity.timestamp
                        )
                    ).awaitSingle()
                    val s = itemRepository.save(item.copy(owner = newOwner, updatedAt = transferActivity.timestamp))
                        .awaitSingle()
                   Pair(s, newOwnership)
                } else Pair(item, null)
            } else {
                val newOwnership = ownershipRepository.save(
                    Ownership(
                        contract = transferActivity.contract,
                        tokenId = transferActivity.tokenId,
                        owner = newOwner,
                        creator = newOwner,
                        date = transferActivity.timestamp
                    )
                ).awaitSingle()
                val saved = itemRepository.save(
                    Item(
                        contract = transferActivity.contract,
                        tokenId = transferActivity.tokenId,
                        creator = newOwner,
                        owner = newOwner,
                        royalties = emptyList(),
                        mintedAt = transferActivity.timestamp,
                        collection = transferActivity.contract,
                        updatedAt = transferActivity.timestamp
                    )
                ).awaitSingle()
                Pair(saved, newOwnership)
            }
        }

        if (needSendToKafka) {
            protocolEventPublisher.onItemUpdate(saved)
            newOwnership?.let { protocolEventPublisher.onUpdate(it) }
        }

    }

}
