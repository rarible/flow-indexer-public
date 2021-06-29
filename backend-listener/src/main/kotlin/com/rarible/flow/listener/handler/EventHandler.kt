package com.rarible.flow.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.OwnershipRepo
import com.rarible.flow.events.EventMessage
import com.rarible.flow.events.NftEvent
import java.time.Instant

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import org.onflow.sdk.bytesToHex
import java.math.BigDecimal


class EventHandler(
    private val itemRepository: ItemRepository,
    private val ownershipRepo: OwnershipRepo,
    private val orderRepository: OrderRepository
) : ConsumerEventHandler<NftEvent> {

    override suspend fun handle(event: NftEvent) {
        val address = event.eventId.contractAddress.bytes.bytesToHex()
        val tokenId = event.id
        when(event) {
            is NftEvent.Burn -> burn(address, tokenId)
            is NftEvent.Deposit -> deposit(address, tokenId, event.to)
            is NftEvent.Mint -> mint(address, tokenId)
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

    private fun withdraw(address: String, tokenId: Int, from: org.onflow.sdk.FlowAddress) {

    }

    private suspend fun mint(address: String, tokenId: Int) {
        val existingEvent = itemRepository.findById(Item.makeId(address, tokenId))
        if (existingEvent == null) {
            itemRepository.save(
                Item(
                    address,
                    tokenId,
                    Address(address),
                    emptyList(),
                    Address(address),
                    Instant.now(),
                    1000, //TODO fix
                    emptyMap()
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
            ownershipRepo.deleteAllByContractAndTokenId(Address(address), tokenId)
        }

        items.await()
        ownerships.await()
    }


    suspend fun deposit(address: String, id: Int, to: org.onflow.sdk.FlowAddress) = coroutineScope {
        val owner = Address(to.bytes.bytesToHex())
        val items = async {
            itemRepository
                .findById(Item.makeId(address, id))
                ?.let {
                    itemRepository.save(it.copy(owner = owner))
                }
        }
        val ownership = async {
            ownershipRepo.findAllByContractAndTokenId(
                Address(address), id
            ).map { it.copy(owner = owner) }.let { ownershipRepo.saveAll(it) }
        }

        items.await()
        ownership.await()
    }

}