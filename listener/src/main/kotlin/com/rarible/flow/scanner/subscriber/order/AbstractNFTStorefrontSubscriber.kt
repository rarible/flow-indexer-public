package com.rarible.flow.scanner.subscriber.order

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowEvent
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.core.apm.withSpan
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.cadence.ListingAvailable
import com.rarible.flow.scanner.cadence.ListingCompleted
import com.rarible.flow.scanner.model.NFTStorefrontEventType
import com.rarible.flow.scanner.model.parse
import com.rarible.flow.scanner.subscriber.BaseFlowLogEventSubscriber
import com.rarible.flow.scanner.subscriber.DescriptorFactory
import javax.annotation.PostConstruct
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking

abstract class AbstractNFTStorefrontSubscriber(
    private val collectionRepository: ItemCollectionRepository,
    private val txManager: TxManager,
    private val orderRepository: OrderRepository
): BaseFlowLogEventSubscriber() {

    private val events = NFTStorefrontEventType.EVENT_NAMES
    protected abstract val name: String
    protected abstract val contract: Contracts

    private lateinit var nftEvents: Set<String>

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = createDescriptors()

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType {
        val eventType = NFTStorefrontEventType.fromEventName(
            EventId.of(log.event.id).eventName
        )
        return when (eventType) {
            NFTStorefrontEventType.LISTING_AVAILABLE -> FlowLogType.LISTING_AVAILABLE
            NFTStorefrontEventType.LISTING_COMPLETED -> FlowLogType.LISTING_COMPLETED
            null -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
        }
    }

    override suspend fun isNewEvent(block: FlowBlockchainBlock, event: FlowEvent): Boolean {
        if (nftEvents.isEmpty()) {
            nftEvents = collectionRepository.findAll().asFlow().toList().flatMap {
                listOf("${it.id}.Withdraw", "${it.id}.Deposit")
            }.toSet()
        }
        return withSpan("checkOrderIsNewEvent", "event") { super.isNewEvent(block, event) && when(EventId.of(event.type).eventName) {
            "ListingAvailable" -> {
                val e = event.event.parse<ListingAvailable>()
                val nftCollection = EventId.of(e.nftType).collection()
                collectionRepository.existsById(nftCollection).awaitSingle()
            }
            "ListingCompleted" -> {
                val e = event.event.parse<ListingCompleted>()
                val orderExists = orderRepository.existsById(e.listingResourceID).awaitSingle()
                return@withSpan orderExists || (e.purchased && txManager.onTransaction(
                    blockHeight = block.number,
                    transactionId = event.transactionId
                ) { result ->
                    result.events.map { EventId.of(it.type) }.any {
                        nftEvents.contains(it.toString())
                    }
                })
            }
            else -> false
        } }
    }

    private fun createDescriptors(): Map<FlowChainId, FlowDescriptor> {
        return FlowChainId.values()
            .mapNotNull { chainId ->
                if (contract.deployments[chainId] != null)
                    chainId to createFlowNftOrderDescriptor(chainId)
                else null
            }
            .toMap()
    }

    private fun createFlowNftOrderDescriptor(chainId: FlowChainId): FlowDescriptor {
        return DescriptorFactory.flowNftOrderDescriptor(
            contract = contract,
            events = events,
            chainId = chainId,
            dbCollection = collection,
            name = name
        )
    }

    @PostConstruct
    private fun postConstruct() = runBlocking {
        nftEvents =
            collectionRepository.findAll().asFlow().toList().flatMap {
                listOf("${it.id}.Withdraw", "${it.id}.Deposit")
            }.toSet()
    }
}
