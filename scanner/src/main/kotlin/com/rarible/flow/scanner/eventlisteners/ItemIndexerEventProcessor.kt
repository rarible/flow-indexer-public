package com.rarible.flow.scanner.eventlisteners

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.ItemService
import com.rarible.flow.scanner.service.OrderService
import com.rarible.flow.scanner.service.OwnershipService
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class ItemIndexerEventProcessor(
    private val itemService: ItemService,
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val ownershipService: OwnershipService,
    private val orderService: OrderService,
): IndexerEventsProcessor {

    private val objectMapper = ObjectMapper()

    override fun isSupported(event: IndexerEvent): Boolean = event.activity is MintActivity ||
            event.activity is WithdrawnActivity || event.activity is DepositActivity || event.activity is BurnActivity

    override suspend fun process(event: IndexerEvent) {
        when(event.activity) {
            is MintActivity -> mintItemEvent(event)
            is WithdrawnActivity -> itemWithdrawn(event)
            is DepositActivity -> itemDeposit(event)
            is BurnActivity -> burnItemEvent(event)
            else -> throw IllegalStateException("Unsupported item event!  [${event.activity::class.simpleName}]")
        }
    }

    private suspend fun mintItemEvent(event: IndexerEvent) {
        val activity = event.activity as MintActivity
        withSpan("mintItemEvent", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val id = ItemId(contract = activity.contract, tokenId = activity.tokenId)
            val owner = FlowAddress(activity.owner)
            val ownershipId = OwnershipId(contract = activity.contract, tokenId = activity.tokenId, owner = owner)
            val itemExists = itemRepository.existsById(id).awaitSingle()
            val ownershipExists = ownershipRepository.existsById(ownershipId).awaitSingle()
            if (!itemExists) {
                val item = itemRepository.insert(
                    Item(
                        contract = id.contract,
                        tokenId = id.tokenId,
                        creator = owner,
                        royalties = activity.royalties,
                        owner = owner,
                        mintedAt = activity.timestamp,
                        meta = objectMapper.writeValueAsString(activity.metadata),
                        collection = activity.contract,
                        updatedAt = activity.timestamp
                    )
                ).awaitSingle()
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onItemUpdate(item)
                }
            } else {
                val item = itemRepository.findById(id).awaitSingle()
                if (item.mintedAt != activity.timestamp) {
                    itemRepository.save(item.copy(mintedAt = activity.timestamp))
                }
            }

            if (!ownershipExists) {
                val ownership = ownershipRepository.insert(
                    Ownership(
                        contract = activity.contract,
                        tokenId = activity.tokenId,
                        owner = owner,
                        creator = owner,
                        date = activity.timestamp
                    )
                ).awaitSingle()
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onUpdate(ownership)
                }
            }
        }
    }

    private suspend fun burnItemEvent(event: IndexerEvent) {
        val activity = event.activity as BurnActivity
        withSpan("burnItemEvent", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val itemId = ItemId(contract = activity.contract, tokenId = activity.tokenId)
            val exists = itemRepository.existsById(itemId).awaitSingle()
            if (exists) {
                val item = itemRepository.findById(itemId).awaitSingle()
                itemRepository.save(item.copy(owner = null, updatedAt = activity.timestamp)).awaitFirstOrNull()
                val ownerships =
                    ownershipRepository.deleteAllByContractAndTokenId(itemId.contract, itemId.tokenId).asFlow().toList()
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onItemDelete(itemId)
                    ownerships.forEach {
                        protocolEventPublisher.onDelete(it)
                    }
                }
            }
        }
    }

    private suspend fun itemWithdrawn(event: IndexerEvent) {
        val activity = event.activity as WithdrawnActivity
        if (activity.from == null) return
        withSpan("itemWithdrawn", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val itemId = ItemId(contract = activity.contract, tokenId = activity.tokenId)
            val from = FlowAddress(activity.from ?: "0x00")
            itemService.withItem(itemId) { item ->
                if (item.updatedAt <= activity.timestamp && item.owner != null) {
                    val saved = itemRepository.coSave(
                        item.copy(
                            owner = from,
                            updatedAt = activity.timestamp
                        )
                    )

                    if (event.source != Source.REINDEX) {
                        protocolEventPublisher.onItemUpdate(saved)
                    }
                }
                val ownershipId = OwnershipId(item.contract, item.tokenId, from)
                ownershipRepository
                    .coFindById(ownershipId)
                    ?.let { ownership ->
                        ownershipService.deleteOwnership(item)
                        if (event.source != Source.REINDEX) {
                            protocolEventPublisher.onDelete(ownership)
                        }
                    }
                val o = orderService.deactivateOrdersByItem(
                    item,
                    LocalDateTime.ofInstant(event.activity.timestamp, ZoneOffset.UTC)
                )
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onUpdate(o)
                }
            }
        }
    }

    private suspend fun itemDeposit(event: IndexerEvent) {
        val activity = event.activity as DepositActivity
        if (activity.to == null) return
        withSpan("itemDeposit", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val itemId = ItemId(contract = activity.contract, tokenId = activity.tokenId)
            val newOwner = FlowAddress(activity.to ?: "0x00")
            val item = itemRepository.coFindById(itemId)
            if (item != null && item.updatedAt <= activity.timestamp) {
                val deposited = itemRepository.coSave(item.copy(owner = newOwner, updatedAt = activity.timestamp))
                val ownership = ownershipService.setOwnershipTo(item, newOwner)
                val o = orderService.restoreOrdersForItem(
                    deposited,
                    LocalDateTime.ofInstant(event.activity.timestamp, ZoneOffset.UTC)
                )
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onItemUpdate(deposited)
                    protocolEventPublisher.onUpdate(ownership)
                    protocolEventPublisher.onUpdate(o)
                }
            }
        }
    }
}
