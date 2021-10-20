package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowActivity
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.cadence.OrderAvailable
import com.rarible.flow.scanner.cadence.ListingCompleted
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

@Component
class OrderSubscriber(
    val itemRepository: ItemRepository,
    val orderRepository: OrderRepository
) : BaseItemHistoryFlowLogSubscriber() {


    override val descriptors = mapOf(
        FlowChainId.MAINNET to FlowDescriptor(
            id = "OrderSubscriber",
            events = setOf(
                "A.665b9acf64dfdfdb.NFTStorefront.ListingAvailable",
                "A.665b9acf64dfdfdb.NFTStorefront.ListingCompleted",
            ),
            collection = collection
        ),
        FlowChainId.TESTNET to FlowDescriptor(
            id = "OrderSubscriber",
            events = setOf(
                "A.01658d9b94068f3c.NFTStorefront.ListingAvailable",
                "A.01658d9b94068f3c.NFTStorefront.ListingCompleted",
            ),
            collection = collection,
            startFrom = 47330085L
        ),
        FlowChainId.EMULATOR to FlowDescriptor(
            id = "MotoGPCardDescriptor",
            events = emptySet(),
            collection = collection,
            startFrom = 1L
        )
    )


    override fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): FlowActivity? {
        val contract = msg.eventId.collection()
        val timestamp = msg.timestamp
        return when (msg.eventId.eventName) {
            "ListingAvailable" -> {
                val event = com.nftco.flow.sdk.Flow.unmarshall(OrderAvailable::class, log.event.event)
                val take = FlowAssetFungible(
                    contract = event.ftVaultType.collection(),
                    value = event.price
                )
                val nftContract = event.nftType.collection()
                val make = FlowAssetNFT(
                    contract = nftContract,
                    value = 1.toBigDecimal(),
                    tokenId = event.nftID
                )
                val itemId = ItemId(nftContract, event.nftID)
                val item = runBlocking {
                    itemRepository.coFindById(itemId)
                }

                if(item != null) {
                    FlowNftOrderActivityList(
                        contract = contract,
                        tokenId = event.nftID,
                        timestamp = timestamp,
                        price = event.price,
                        make = make,
                        take = take,
                        hash = event.listingResourceID.toString(),
                        maker = item.owner?.formatted ?: ""
                    )
                } else {
                    logger.warn("Listing available event [$event] for non-existing item [$itemId]")
                    null
                }
            }
            "ListingCompleted" -> {
                val event = com.nftco.flow.sdk.Flow.unmarshall(ListingCompleted::class, log.event.event)
                val order = runBlocking {
                    orderRepository.coFindById(event.listingResourceID)
                }

                return if(order == null) {
                    logger.warn("ListingCompleted event [$event] for non-existing order")
                    null
                } else {
                    val item = runBlocking {
                        itemRepository.coFindById(order.itemId)
                    }
                    if (event.purchased) {
                        FlowNftOrderActivitySell(
                            // TODO
                        )
                    } else {
                        FlowNftOrderActivityCancelList(
                            price = order.amount,
                            hash = order.id.toString(),
                            maker = item?.owner!!.formatted,
                            make = order.make,
                            take = order.take,
                            contract = order.itemId.contract,
                            tokenId = order.itemId.tokenId,
                            timestamp = timestamp
                        )
                    }
                }
            }
            else -> throw IllegalStateException("Unsupported eventId: ${msg.eventId}" )
        }
    }

    companion object {
        val logger by Log()
    }
}