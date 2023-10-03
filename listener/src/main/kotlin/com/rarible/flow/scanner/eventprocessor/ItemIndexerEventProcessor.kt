package com.rarible.flow.scanner.eventprocessor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.common.EventTimeMarks
import com.rarible.core.common.optimisticLock
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.OrderService
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.kotlin.extra.math.max
import java.time.Instant

@Component
class ItemIndexerEventProcessor(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderService: OrderService,
) : IndexerEventsProcessor {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val objectMapper = jacksonObjectMapper()

    private val supportedTypes = arrayOf(FlowActivityType.MINT, FlowActivityType.TRANSFER, FlowActivityType.BURN)

    override fun isSupported(event: IndexerEvent): Boolean = event.activityType() in supportedTypes

    override suspend fun process(event: IndexerEvent) {
        val marks = event.eventTimeMarks
        when (event.activityType()) {
            FlowActivityType.MINT -> mintItemEvent(event, marks)
            FlowActivityType.TRANSFER -> transfer(event, marks)
            FlowActivityType.BURN -> burnItemEvent(event, marks)
            else -> throw IllegalStateException("Unsupported item event!  [${event.activityType()}]")
        }
    }

    suspend fun mintItemEvent(event: IndexerEvent, marks: EventTimeMarks) {
        val mintActivity = event.history.activity as MintActivity

        val owner = FlowAddress(mintActivity.owner)
        val creator = if (mintActivity.creator != null) FlowAddress(mintActivity.creator!!) else owner
        val forSave = if (event.item == null) {
            Item(
                contract = mintActivity.contract,
                tokenId = mintActivity.tokenId,
                creator = creator,
                royalties = mintActivity.royalties,
                owner = owner,
                mintedAt = mintActivity.timestamp,
                meta = objectMapper.writeValueAsString(mintActivity.metadata),
                collection = mintActivity.collection ?: mintActivity.contract,
                updatedAt = mintActivity.timestamp
            )
        } else if (event.item.mintedAt != mintActivity.timestamp) {
            event.item.copy(
                mintedAt = mintActivity.timestamp,
                creator = creator,
                royalties = mintActivity.royalties,
                meta = objectMapper.writeValueAsString(mintActivity.metadata),
            )
        } else event.item

        if (forSave != event.item) {
            val saved = itemRepository.save(forSave).awaitSingle()
            val needSendToKafka = willSendToKafka(event)
            if (needSendToKafka) {
                protocolEventPublisher.onItemUpdate(saved, marks)
            }
            if (saved.updatedAt <= mintActivity.timestamp) {
                // we should not have ownership records yet
                val deleted = ownershipRepository.deleteAllByContractAndTokenId(
                    forSave.contract, forSave.tokenId
                ).asFlow().toList()

                deleted.map {
                    orderService.deactivateOrdersByOwnership(
                        it,
                        mintActivity.timestamp,
                        needSendToKafka,
                        marks
                    )
                }
                val o = saveOwnership(
                    Ownership(
                        contract = mintActivity.contract,
                        tokenId = mintActivity.tokenId,
                        owner = owner,
                        creator = creator,
                        date = mintActivity.timestamp
                    )
                )
                orderService.restoreOrdersForOwnership(o, mintActivity.timestamp, needSendToKafka, marks)
                if (needSendToKafka) {
                    protocolEventPublisher.onDelete(deleted, marks)
                    protocolEventPublisher.onUpdate(o, marks)
                }
            } else {
                ownershipRepository.findAllByContractAndTokenId(forSave.contract, forSave.tokenId)
                    .max { o1, o2 -> o1.date.compareTo(o2.date) }
                    .awaitSingleOrNull()
                    ?.let { o ->
                        ownershipRepository
                            .deleteAllByContractAndTokenIdAndOwnerNot(forSave.contract, forSave.tokenId, o.owner)
                            .asFlow()
                            .onEach {
                                if (needSendToKafka) {
                                    orderService.deactivateOrdersByOwnership(
                                        it,
                                        mintActivity.timestamp,
                                        true,
                                        marks
                                    )
                                }
                            }
                            .toList()
                        if (o.creator != creator) {
                            val ownership = saveOwnership(o.copy(creator = creator))
                            if (needSendToKafka) {
                                protocolEventPublisher.onUpdate(ownership, marks)
                            }
                        }
                    }
            }
        }
    }

    private suspend fun burnItemEvent(event: IndexerEvent, marks: EventTimeMarks) {
        val burn = event.history.activity as BurnActivity
        val item = event.item

        val needSendToKafka = willSendToKafka(event)
        if (item != null && item.updatedAt <= burn.timestamp) {
            itemRepository.save(item.copy(owner = null, updatedAt = burn.timestamp)).awaitFirstOrNull()
            val ownerships = ownershipRepository.deleteAllByContractAndTokenId(
                item.contract, item.tokenId
            ).asFlow().toList()
            ownerships.forEach {
                orderService.deactivateOrdersByOwnership(
                    it,
                    burn.timestamp,
                    needSendToKafka,
                    marks
                )
            }
            if (needSendToKafka) {
                protocolEventPublisher.onItemDelete(item.id, marks)
                protocolEventPublisher.onDelete(ownerships, marks)
            }
        } else if (item == null) {
            val saved = itemRepository.insert(
                Item(
                    contract = burn.contract,
                    tokenId = burn.tokenId,
                    royalties = listOf(),
                    creator = EventId.of("${burn.contract}.dummy").contractAddress,
                    owner = null,
                    mintedAt = Instant.now(),
                    collection = burn.contract,
                    updatedAt = burn.timestamp
                )
            ).awaitSingle()
            if (needSendToKafka) {
                protocolEventPublisher.onItemDelete(saved.id, marks)
            }
        }
    }

    private suspend fun transfer(event: IndexerEvent, eventTimeMarks: EventTimeMarks) {
        val transferActivity = event.history.activity as TransferActivity

        val item = event.item
        val needSendToKafka = willSendToKafka(event)
        val prevOwner = FlowAddress(transferActivity.from)
        val newOwner = FlowAddress(transferActivity.to)

        val prevOwnership = ownershipRepository.findById(
            OwnershipId(
                contract = transferActivity.contract,
                tokenId = transferActivity.tokenId,
                owner = prevOwner
            )
        ).awaitSingleOrNull()

        if (prevOwnership != null) {
            ownershipRepository.delete(prevOwnership).awaitSingleOrNull()
            orderService.deactivateOrdersByOwnership(
                prevOwnership,
                transferActivity.timestamp,
                needSendToKafka,
                eventTimeMarks
            )
            if (needSendToKafka) {
                protocolEventPublisher.onDelete(prevOwnership, eventTimeMarks)
            }
        }

        val (saved, newOwnership) = if (item != null) {
            if (item.updatedAt <= transferActivity.timestamp && item.owner != null) {
                val newOwnership = saveOwnership(
                    Ownership(
                        contract = transferActivity.contract,
                        tokenId = transferActivity.tokenId,
                        owner = newOwner,
                        creator = item.creator,
                        date = transferActivity.timestamp
                    )
                )
                val updatedItem =
                    itemRepository.save(item.copy(owner = newOwner, updatedAt = transferActivity.timestamp))
                        .awaitSingle()

                orderService.restoreOrdersForOwnership(
                    newOwnership,
                    transferActivity.timestamp,
                    needSendToKafka,
                    eventTimeMarks
                )
                Pair(updatedItem, newOwnership)
            } else Pair(item, null)
        } else {
            val newOwnership = saveOwnership(
                Ownership(
                    contract = transferActivity.contract,
                    tokenId = transferActivity.tokenId,
                    owner = newOwner,
                    creator = newOwner,
                    date = transferActivity.timestamp
                )
            )
            orderService.restoreOrdersForOwnership(
                newOwnership,
                transferActivity.timestamp,
                needSendToKafka,
                eventTimeMarks
            )
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

        orderService.checkAndEnrichTransfer(
            event.history.log.transactionHash,
            prevOwner.formatted,
            newOwner.formatted
        )?.also { sendHistoryUpdate(event, it, eventTimeMarks) }

        if (needSendToKafka) {
            protocolEventPublisher.onItemUpdate(saved, eventTimeMarks)
            newOwnership?.let { protocolEventPublisher.onUpdate(it, eventTimeMarks) }
        }
    }

    private suspend fun saveOwnership(ownership: Ownership) = optimisticLock {
        val existedOwnership = ownershipRepository.findById(ownership.id).awaitFirstOrNull()
        ownershipRepository.save(ownership.copy(version = existedOwnership?.version)).awaitSingle()
    }

    private suspend fun sendHistoryUpdate(
        event: IndexerEvent,
        itemHistory: ItemHistory,
        eventTimeMarks: EventTimeMarks
    ) {
        if (willSendToKafka(event)) {
            protocolEventPublisher.activity(
                history = itemHistory,
                reverted = false,
                eventTimeMarks = eventTimeMarks
            )
        }
    }

    private fun willSendToKafka(event: IndexerEvent): Boolean {
        return true // event.source != Source.REINDEX - TODO
    }
}
