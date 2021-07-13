package com.rarible.flow.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.EventMessage
import com.rarible.flow.events.NftEvent
import com.rarible.flow.log.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import org.onflow.sdk.FlowAddress
import org.onflow.sdk.bytesToHex
import java.math.BigDecimal
import java.time.Instant


class EventHandler(
    private val itemRepository: ItemRepository,
    private val itemReactiveRepository: ItemReactiveRepository,
    private val ownershipRepository: OwnershipRepository,
    private val orderRepository: OrderRepository,
    private val orderReactiveRepository: OrderReactiveRepository
) : ConsumerEventHandler<EventMessage> {

    override suspend fun handle(event: EventMessage) {
        val nftEvent = event.convert()
        if (nftEvent != null) {
            handle(nftEvent)
        } else {
            log.warn(
                "Failed to convert message [$event] to NftEvent. Probably not NFT event, or contract is not supported."
            )
        }
    }

    private suspend fun handle(event: NftEvent) {
        val address = event.eventId.contractAddress.bytes.bytesToHex()
        val tokenId = event.id
        when(event) {
            is NftEvent.Destroy -> burn(address, tokenId)
            is NftEvent.Deposit -> deposit(address, tokenId, event.to)
            is NftEvent.Mint -> mint(address, tokenId, event.to)
            is NftEvent.Withdraw -> withdraw(address, tokenId, event.from)
            is NftEvent.Bid -> bid(address, tokenId, event.bidder, event.amount)
            is NftEvent.List -> list(address, tokenId)
            is NftEvent.Unlist -> unlist(address, tokenId)
            is NftEvent.BidNft -> bidNft(address, tokenId, event.bidder, event.offeredNftAddress, event.offeredNftId)
            is NftEvent.OrderOpened -> orderOpened(event, address)
            is NftEvent.OrderClosed -> orderClosed(event, address)
            is NftEvent.OrderWithdraw -> orderWithdraw(event, address)
            is NftEvent.OrderAssigned -> orderAssigned(event, address)
        }
    }

    private fun orderAssigned(event: NftEvent.OrderAssigned, address: String) {
        orderReactiveRepository.findById(event.id).subscribe {
            orderReactiveRepository.save(
                it.copy(taker = Address(event.to.formatted))
            )
        }
    }

    private suspend fun orderWithdraw(event: NftEvent.OrderWithdraw, address: String) {
    }

    private fun orderClosed(event: NftEvent.OrderClosed, address: String) {
        orderReactiveRepository.findById(event.id).subscribe {
            orderReactiveRepository.save(
                it.copy(
                    fill = 1
                )
            )
        }
    }

    private fun orderOpened(event: NftEvent.OrderOpened, address: String) {
        orderReactiveRepository.save(
            Order(
                id = event.id,
                itemId = Item.makeId(address, event.askId),
                maker = Address(event.maker.formatted),
                amount = event.bidAmount
            )
        ).subscribe { order ->
            itemReactiveRepository.findById(order.itemId).subscribe { item ->
                itemReactiveRepository.save(
                    item.copy(
                        listed = true
                    )
                )
            }
        }



    }

    private suspend fun bidNft(
        address: String,
        tokenId: ULong,
        bidder: FlowAddress,
        offeredNftAddress: FlowAddress,
        offeredNftId: Int
    ) {
        /*withItem(address, tokenId) { myNft ->
            withItem(offeredNftAddress.formatted, offeredNftId) { theirNft ->
                orderRepository.save(
                    Order(
                        ObjectId(),
                        Item.makeId(address, tokenId),
                        Address(bidder.bytes.bytesToHex()),
                        1.toBigDecimal(),
                        theirNft.id
                    )
                )
            }
        }*/
    }

    private suspend fun unlist(address: String, tokenId: ULong) {
        update(address, tokenId) {
            it.copy(listed = false)
        }
    }

    private suspend fun list(address: String, tokenId: ULong) {
        update(address, tokenId) {
            it.copy(listed = true)
        }
    }

    private suspend fun bid(address: String, tokenId: ULong, bidder: FlowAddress, amount: BigDecimal) {
        /*withItem(address, tokenId) {
            orderRepository.save(
                Order(
                    ObjectId(),
                    Item.makeId(address, tokenId),
                    Address(bidder.bytes.bytesToHex()),
                    amount
                )
            )
        }*/
    }

    private fun withdraw(address: String, tokenId: ULong, from: FlowAddress) {

    }

    private suspend fun mint(contract: String, tokenId: ULong, to: FlowAddress) {
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

    suspend fun update(address: String, tokenId: ULong, fn: suspend (Item) -> Item) {
        withItem(address, tokenId) {
            itemRepository.save(fn(it))
        }
    }

    suspend fun <T> withItem(address: String, tokenId: ULong, fn: suspend (Item) -> T) {
        val existingEvent = itemRepository.findById(Item.makeId(address, tokenId))
        if(existingEvent != null) {
            fn(existingEvent)
        }
    }

    suspend fun burn(address: String, tokenId: ULong) = coroutineScope{
        val items = async { itemRepository.delete(Item.makeId(address, tokenId)) }
        val ownerships = async {
            ownershipRepository.deleteAllByContractAndTokenId(Address(address), tokenId)
        }

        items.await()
        ownerships.await()
    }


    suspend fun deposit(address: String, id: ULong, to: FlowAddress) = coroutineScope {
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
