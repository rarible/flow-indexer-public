package com.rarible.flow.scanner.eventlisteners

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.ItemService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ItemEventListeners(
    private val itemService: ItemService,
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository, //TODO should be removed
    private val protocolEventPublisher: ProtocolEventPublisher,
) {

    private val objectMapper = ObjectMapper()

    // TODO cleanup code
    @EventListener(condition = "#event.activity instanceof T(com.rarible.flow.core.domain.MintActivity)")
    fun mintItemEvent(event: IndexerEvent) = runBlocking {
        val activity = event.activity as MintActivity
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
                itemRepository.save(item.copy(mintedAt = activity.timestamp)).subscribe()
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

    @EventListener(condition = "#event.activity instanceof T(com.rarible.flow.core.domain.BurnActivity)")
    fun burnItemEvent(event: IndexerEvent) = runBlocking {
        val activity = event.activity as BurnActivity
        val itemId = ItemId(contract = activity.contract, tokenId = activity.tokenId)
        val exists = itemRepository.existsById(itemId).awaitSingle()
        if (exists) {
            val item = itemRepository.findById(itemId).awaitSingle()
            itemRepository.save(item.copy(owner = null, updatedAt = activity.timestamp)).subscribe()
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

    @EventListener(condition = "#event.activity instanceof T(com.rarible.flow.core.domain.WithdrawnActivity)")
    fun itemWithdrawn(event: IndexerEvent): ItemIsWithdrawn? = runBlocking {
        val activity = event.activity as WithdrawnActivity
        val itemId = ItemId(contract = activity.contract, tokenId = activity.tokenId)
        val from = FlowAddress(activity.from ?: "0x00")
        itemService.withItem(itemId) { item ->
            if (item.updatedAt <= activity.timestamp) {
                protocolEventPublisher.onItemUpdate(
                    itemRepository.coSave(
                        item.copy(
                            owner = from,
                            updatedAt = activity.timestamp
                        )
                    )
                )
            }
            ItemIsWithdrawn(item, from, activity.timestamp, event.source)
        }
    }

    @EventListener(condition = "#event.activity instanceof T(com.rarible.flow.core.domain.DepositActivity)")
    fun itemDeposit(event: IndexerEvent): ItemIsDeposited? = runBlocking {
        val activity = event.activity as DepositActivity
        val itemId = ItemId(contract = activity.contract, tokenId = activity.tokenId)
        val newOwner = FlowAddress(activity.to ?: "0x00")
        itemService.withItem(itemId) { item ->
            if (item.updatedAt <= activity.timestamp) {
                val deposited = itemRepository.coSave(item.copy(owner = newOwner, updatedAt = activity.timestamp))
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onItemUpdate(deposited)
                }
                ItemIsDeposited(deposited, newOwner, item.owner, activity.timestamp, event.source)
            } else {
                null
            }
        }
    }
}
