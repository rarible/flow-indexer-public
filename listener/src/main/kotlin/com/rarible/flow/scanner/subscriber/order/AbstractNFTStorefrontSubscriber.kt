package com.rarible.flow.scanner.subscriber.order

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowEvent
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.scanner.cadence.ListingAvailable
import com.rarible.flow.scanner.cadence.ListingCompleted
import com.rarible.flow.scanner.model.NFTStorefrontEventType
import com.rarible.flow.scanner.model.parse
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import com.rarible.flow.scanner.subscriber.BaseFlowLogEventSubscriber
import com.rarible.flow.scanner.subscriber.DescriptorFactory
import kotlinx.coroutines.reactor.awaitSingle

abstract class AbstractNFTStorefrontSubscriber(
    supportedNftCollectionProvider: SupportedNftCollectionProvider,
    private val orderRepository: OrderRepository,
    chainId: FlowChainId,
) : BaseFlowLogEventSubscriber(chainId) {

    private val events = NFTStorefrontEventType.EVENT_NAMES
    protected abstract val name: String
    protected abstract val contract: Contracts

    private val nftCollections = supportedNftCollectionProvider.get()

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = createDescriptors()

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType {
        return convertToLogType(log.event)
    }

    override suspend fun isNewEvent(block: FlowBlockchainBlock, event: FlowEvent): Boolean {
        return super.isNewEvent(block, event) && when (convertToLogType(event)) {
            FlowLogType.LISTING_AVAILABLE -> {
                val e = event.event.parse<ListingAvailable>()
                e.nftCollection() in nftCollections
            }

            FlowLogType.LISTING_COMPLETED -> {
                val e = event.event.parse<ListingCompleted>()
                val orderExists = orderRepository.existsById(e.listingResourceID).awaitSingle()
                orderExists || e.nftCollection() in nftCollections
            }

            else -> false
        }
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

    private fun convertToLogType(event: FlowEvent): FlowLogType {
        val eventType = NFTStorefrontEventType.fromEventName(
            EventId.of(event.id).eventName
        )
        return when (eventType) {
            NFTStorefrontEventType.LISTING_AVAILABLE -> FlowLogType.LISTING_AVAILABLE
            NFTStorefrontEventType.LISTING_COMPLETED -> FlowLogType.LISTING_COMPLETED
            null -> throw IllegalStateException("Unsupported event type: ${event.id}")
        }
    }
}
