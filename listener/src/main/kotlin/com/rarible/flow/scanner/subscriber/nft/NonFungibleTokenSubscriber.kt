package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.model.NonFungibleTokenEventType
import com.rarible.flow.scanner.subscriber.BaseFlowLogEventSubscriber
import com.rarible.flow.scanner.subscriber.DescriptorFactory

abstract class NonFungibleTokenSubscriber(chainId: FlowChainId) : BaseFlowLogEventSubscriber(chainId) {
    protected open val events = NonFungibleTokenEventType.EVENT_NAMES

    protected abstract val name: String
    protected abstract val contract: Contracts

    protected open fun fromEventName(eventName: String) = NonFungibleTokenEventType.fromEventName(eventName)

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = createDescriptors()

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType {
        val eventType = fromEventName(EventId.of(log.event.id).eventName)
        return when (eventType) {
            NonFungibleTokenEventType.WITHDRAW -> FlowLogType.WITHDRAW
            NonFungibleTokenEventType.DEPOSIT -> FlowLogType.DEPOSIT
            NonFungibleTokenEventType.MINT -> FlowLogType.MINT
            NonFungibleTokenEventType.BURN -> FlowLogType.BURN
            null -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
        }
    }

    private fun createDescriptors(): Map<FlowChainId, FlowDescriptor> {
        return FlowChainId.values()
            .mapNotNull { chainId ->
                if (contract.deployments[chainId] != null) chainId to createFlowNftOrderDescriptor(chainId) else null
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
}
