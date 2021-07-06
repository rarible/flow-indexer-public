package com.rarible.flow.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.events.EventMessage
import com.rarible.flow.events.NftEvent
import com.rarible.flow.log.Log
import java.time.Instant

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import org.onflow.sdk.bytesToHex
import java.math.BigDecimal


class EventHandler(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val orderRepository: OrderRepository
) : ConsumerEventHandler<EventMessage> {

    override suspend fun handle(event: EventMessage) {
        val nftEvent = event.convert()
        if (nftEvent != null) {
            handle(nftEvent)
        } else {
            log.warn(
                "Failed to convert message [$this] to NftEvent. Probably not NFT event, or contract is not supported."
            )
        }
    }

    private suspend fun handle(event: NftEvent) {
        val address = event.eventId.contractAddress.bytes.bytesToHex()
        val tokenId = event.id
        when(event) {
            is NftEvent.Burn -> burn(address, tokenId)
            is NftEvent.Deposit -> deposit(address, tokenId, event.to)
            is NftEvent.Mint -> mint(address, tokenId, event.to)
            is NftEvent.Withdraw -> withdraw(address, tokenId, event.from)
            is NftEvent.Bid -> bid(address, tokenId, event.bidder, event.amount)
            is NftEvent.List -> list(address, tokenId)
            is NftEvent.Unlist -> unlist(address, tokenId)
        }
    }

    private suspend fun unlist(address: String, tokenId: Int) {
        update(address, tokenId) {
            it.copy(listed = false)
        }
    }

    private suspend fun list(address: String, tokenId: Int) {
        update(address, tokenId) {
            it.copy(listed = true)
        }
    }

    private suspend fun bid(address: String, tokenId: Int, bidder: FlowAddress, amount: BigDecimal) {
        withItem(address, tokenId) {
            orderRepository.save(
                Order(
                    ObjectId(),
                    Item.makeId(address, tokenId),
                    Address(bidder.bytes.bytesToHex()),
                    amount
                )
            )
        }
    }

    private fun withdraw(address: String, tokenId: Int, from: FlowAddress) {

    }

    private suspend fun mint(contract: String, tokenId: Int, to: FlowAddress) {
        val existingEvent = itemRepository.findById(Item.makeId(contract, tokenId))
        if (existingEvent == null) {
            itemRepository.save(
                Item(
                    contract,
                    tokenId,
                    Address(to.formatted),
                    emptyList(),
                    Address(to.formatted),
                    Instant.now(),
                    emptyMap()
                )
            )

            ownershipRepository.save(
                Ownership(
                    Address(contract),
                    tokenId,
                    Address(to.formatted),
                    Instant.now()
                )
            )
        }
    }

    suspend fun update(address: String, tokenId: Int, fn: suspend (Item) -> Item) {
        withItem(address, tokenId) {
            itemRepository.save(fn(it))
        }
    }

    suspend fun <T> withItem(address: String, tokenId: Int, fn: suspend (Item) -> T) {
        val existingEvent = itemRepository.findById(Item.makeId(address, tokenId))
        if(existingEvent != null) {
            fn(existingEvent)
        }
    }

    suspend fun burn(address: String, tokenId: Int) = coroutineScope{
        val items = async { itemRepository.delete(Item.makeId(address, tokenId)) }
        val ownerships = async {
            ownershipRepository.deleteAllByContractAndTokenId(Address(address), tokenId)
        }

        items.await()
        ownerships.await()
    }


    suspend fun deposit(address: String, id: Int, to: FlowAddress) = coroutineScope {
        val owner = Address(to.bytes.bytesToHex())
        val items = async {
            itemRepository
                .findById(Item.makeId(address, id))
                ?.let {
                    itemRepository.save(it.copy(owner = owner))
                }
        }
        val ownership = async {
            ownershipRepository.findAllByContractAndTokenId(
                Address(address), id
            ).map { it.copy(owner = owner) }.let { ownershipRepository.saveAll(it) }
        }

        items.await()
        ownership.await()
    }

    companion object {
        val log by Log()
    }
}