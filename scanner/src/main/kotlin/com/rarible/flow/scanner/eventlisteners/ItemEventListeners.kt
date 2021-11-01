package com.rarible.flow.scanner.eventlisteners

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.DepositActivity
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.WithdrawnActivity
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.scanner.service.ItemService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ItemEventListeners(
    val itemService: ItemService,
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository, //TODO should be removed
    private val protocolEventPublisher: ProtocolEventPublisher,
) {

    // TODO cleanup code
    @EventListener(MintActivity::class)
    fun mintItemEvent(activity: MintActivity) = runBlocking {
        val id = ItemId(contract = activity.contract, tokenId = activity.tokenId)
        val owner = FlowAddress(activity.owner)
        val ownershipId = OwnershipId(contract = activity.contract, tokenId = activity.tokenId, owner = owner)
        val itemExists = itemRepository.existsById(id).awaitSingle()
        val ownershipExists = ownershipRepository.existsById(ownershipId).awaitSingle()
        val mapper = ObjectMapper()
        if (!itemExists) {
            val item = itemRepository.insert(
                Item(
                    contract = id.contract,
                    tokenId = id.tokenId,
                    creator = owner,
                    royalties = activity.royalties,
                    owner = owner,
                    mintedAt = activity.timestamp,
                    meta = mapper.writeValueAsString(activity.metadata),
                    collection = activity.contract,
                    updatedAt = activity.timestamp
                )
            ).awaitSingle()
            protocolEventPublisher.onItemUpdate(item)
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
            protocolEventPublisher.onUpdate(ownership)
        }
    }

    @EventListener(BurnActivity::class)
    fun burnItemEvent(activity: BurnActivity) = runBlocking {
        val itemId = ItemId(contract = activity.contract, tokenId = activity.tokenId)
        val exists = itemRepository.existsById(itemId).awaitSingle()
        if (exists) {
            val item = itemRepository.findById(itemId).awaitSingle()
            itemRepository.save(item.copy(owner = null, updatedAt = activity.timestamp)).awaitSingle()
            protocolEventPublisher.onItemDelete(itemId)
            val ownerships =
                ownershipRepository.deleteAllByContractAndTokenId(itemId.contract, itemId.tokenId).collectList()
                    .awaitSingle()
            ownerships.forEach {
                protocolEventPublisher.onDelete(it)
            }
        }
    }

    @EventListener(WithdrawnActivity::class)
    fun itemWithdrawn(activity: WithdrawnActivity): ItemIsWithdrawn? = runBlocking {
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
            ItemIsWithdrawn(item, from, activity.timestamp)
        }
    }

    @EventListener(DepositActivity::class)
    fun itemDeposit(activity: DepositActivity): ItemIsDeposited? = runBlocking {
        val itemId = ItemId(contract = activity.contract, tokenId = activity.tokenId)
        val newOwner = FlowAddress(activity.to ?: "0x00")
        itemService.withItem(itemId) { item ->
            if (item.updatedAt <= activity.timestamp) {
                val deposited = itemRepository.coSave(item.copy(owner = newOwner, updatedAt = activity.timestamp))
                protocolEventPublisher.onItemUpdate(deposited)
                ItemIsDeposited(deposited, newOwner, item.owner, activity.timestamp)
            } else {
                null
            }
        }
    }
}
