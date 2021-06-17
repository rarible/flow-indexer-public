package com.rarible.flow.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepo
import com.rarible.flow.events.EventMessage
import com.rarible.flow.events.NftEvent
import org.onflow.sdk.toHex
import java.time.Instant

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map


class EventHandler(
    private val itemRepository: ItemRepository,
    private val ownershipRepo: OwnershipRepo
) : ConsumerEventHandler<NftEvent> {

    override suspend fun handle(event: NftEvent) {
        val address = event.eventId.contractAddress.bytes.toHex()
        val tokenId = event.id
        when(event) {
            is NftEvent.Burn -> burn(address, tokenId)
            is NftEvent.Deposit -> deposit(address, tokenId, event.to)
            is NftEvent.Mint -> mint(address, tokenId)
            is NftEvent.Withdraw -> withdraw(address, tokenId, event.from)
        }
    }

    private fun withdraw(address: String, tokenId: Int, from: org.onflow.sdk.Address) {

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

    suspend fun burn(address: String, tokenId: Int) = coroutineScope{
        val items = async { itemRepository.delete(Item.makeId(address, tokenId)) }
        val ownerships = async {
            ownershipRepo.deleteAllByContractAndTokenId(Address(address), tokenId)
        }

        items.await()
        ownerships.await()
    }


    suspend fun deposit(address: String, id: Int, to: org.onflow.sdk.Address) = coroutineScope {
        val owner = Address(to.bytes.toHex())
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