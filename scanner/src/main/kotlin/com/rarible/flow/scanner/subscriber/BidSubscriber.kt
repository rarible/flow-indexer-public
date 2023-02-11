package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowEvent
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.cadence.BidAvailable
import com.rarible.flow.scanner.cadence.BidCompleted
import com.rarible.flow.scanner.model.parse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class BidSubscriber(
    private val collectionRepository: ItemCollectionRepository,
    private val txManager: TxManager,
    private val orderRepository: OrderRepository,
) : BaseFlowLogEventSubscriber() {
    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.TESTNET to flowOrderDescriptor(
                address = "1d56d7ba49283a88",
                contract = "RaribleOpenBid",
                events = listOf("BidAvailable", "BidCompleted"),
                dbCollection = collection
            ),
            FlowChainId.MAINNET to flowOrderDescriptor(
                address = "01ab36aaf654a13e",
                contract = "RaribleOpenBid",
                events = listOf("BidAvailable", "BidCompleted"),
                dbCollection = collection
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when (EventId.of(log.event.id).eventName) {
        "BidAvailable" -> FlowLogType.BID_AVAILABLE
        "BidCompleted" -> FlowLogType.BID_COMPLETED
        else -> throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }

    private lateinit var nftEvents: Set<String>

    override suspend fun isNewEvent(block: FlowBlockchainBlock, event: FlowEvent): Boolean {
        if (nftEvents.isEmpty()) {
            nftEvents = collectionRepository.findAll().asFlow().toList().flatMap {
                listOf("${it.id}.Withdraw", "${it.id}.Deposit")
            }.toSet()
        }
        return withSpan("checkBidIsNewEvent", "event") { super.isNewEvent(block, event) && when(EventId.of(event.type).eventName) {
            "BidAvailable" -> {
                val e = event.event.parse<BidAvailable>()
                collectionRepository.existsById(e.nftType.collection()).awaitSingle()
            }
            "BidCompleted" -> {
                val e = event.event.parse<BidCompleted>()
                if (nftEvents.isEmpty()) {
                    nftEvents = collectionRepository.findAll().asFlow().toList().flatMap {
                        listOf("${it.id}.Withdraw", "${it.id}.Deposit")
                    }.toSet()
                }
                return@withSpan if (e.purchased) {
                    txManager.onTransaction(
                        blockHeight = block.number,
                        transactionId = event.transactionId
                    ) {
                        it.events.map { EventId.of(it.type) }.any {
                            nftEvents.contains(it.toString())
                        }
                    }
                } else orderRepository.existsById(e.bidId).awaitSingle()
            }
            else -> false
        } }
    }

    @PostConstruct
    private fun postConstruct() = runBlocking {
        nftEvents =
            collectionRepository.findAll().asFlow().toList().flatMap {
                listOf("${it.id}.Withdraw", "${it.id}.Deposit")
            }.toSet()
    }
}
