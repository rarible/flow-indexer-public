package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.cadence.ListingCompleted
import com.rarible.flow.scanner.cadence.NFTDeposit
import com.rarible.flow.scanner.cadence.OrderAvailable
import com.rarible.flow.scanner.model.parse
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OrderSubscriber(
    val orderRepository: OrderRepository,
    val txManager: TxManager,
) : BaseItemHistoryFlowLogSubscriber() {

    override val descriptors = mapOf(
        FlowChainId.MAINNET to FlowDescriptor(
            id = "OrderSubscriber",
            events = setOf(
                "A.b085d2941bebf9c4.CommonOrder.OrderAvailable",
                "A.4eb8a10cb9f87357.NFTStorefront.ListingCompleted",
            ),
            collection = collection
        ),
        FlowChainId.TESTNET to FlowDescriptor(
            id = "OrderSubscriber",
            events = setOf(
                "A.01658d9b94068f3c.CommonOrder.OrderAvailable",
                "A.94b06cfca1d8a476.NFTStorefront.ListingCompleted",
            ),
            collection = collection,
            startFrom = 47330085L
        ),
        FlowChainId.EMULATOR to FlowDescriptor(
            id = "OrderSubscriber",
            events = setOf(
                "A.f8d6e0586b0a20c7.CommonOrder.OrderAvailable",
                "A.f8d6e0586b0a20c7.NFTStorefront.ListingCompleted",
            ),
            collection = collection,
        )
    )


    override fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): FlowActivity? {
        val contract = msg.eventId.collection()
        val timestamp = Instant.ofEpochMilli(block.timestamp)
        return when (msg.eventId.eventName) {
            "OrderAvailable" -> {
                val event = log.event.parse<OrderAvailable>()
                val take = FlowAssetFungible(
                    contract = event.vaultType.collection(),
                    value = event.price
                )
                val make = FlowAssetNFT(
                    contract = event.nftType.collection(),
                    value = 1.toBigDecimal(),
                    tokenId = event.nftId
                )
                FlowNftOrderActivityList(
                    contract = contract,
                    tokenId = event.nftId,
                    timestamp = timestamp,
                    price = event.price,
                    make = make,
                    take = take,
                    hash = event.orderId.toString(),
                    maker = event.orderAddress.formatted
                )
            }
            "ListingCompleted" -> {
                val event = log.event.parse<ListingCompleted>()
                val order = runBlocking {
                    orderRepository.coFindById(event.listingResourceID)
                }
                if (order == null) {
                    logger.warn("ListingCompleted event [$event] for non-existing order")
                    null
                } else {
                    val tokenId = order.itemId.tokenId
                    val tokenContract = order.itemId.contract

                    if (event.purchased) { // order closed
                        // take address from NonFungibleToken.Deposit in the same transaction
                        val buyerAddress = txManager.onTransaction(log.event.transactionId) { tx ->
                            tx.events.asSequence()
                                .filter { it.id.endsWith("$tokenContract.Deposit") }
                                .map { it.parse<NFTDeposit>() }
                                .findLast { it.id == tokenId && it.to != null }
                        }?.to ?: throw IllegalStateException("Can't take buyer address!")

                        FlowNftOrderActivitySell(
                            price = order.amount,
                            tokenId = tokenId,
                            contract = tokenContract,
                            timestamp = timestamp,
                            left = OrderActivityMatchSide(order.maker.formatted, order.make),
                            right = OrderActivityMatchSide(buyerAddress, order.take)
                        )
                    } else { // order cancelled
                        FlowNftOrderActivityCancelList(
                            price = order.amount,
                            hash = order.id.toString(),
                            maker = order.maker.formatted,
                            make = order.make,
                            take = order.take,
                            contract = tokenContract,
                            tokenId = tokenId,
                            timestamp = timestamp
                        )
                    }
                }
            }
            else -> throw IllegalStateException("Unsupported eventId: ${msg.eventId}")
        }
    }

    companion object {
        val logger by Log()
    }
}
