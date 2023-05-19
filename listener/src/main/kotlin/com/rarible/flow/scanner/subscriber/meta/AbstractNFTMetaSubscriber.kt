package com.rarible.flow.scanner.subscriber.meta

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.BaseFlowLogEventSubscriber
import com.rarible.flow.scanner.subscriber.DescriptorFactory

abstract class AbstractNFTMetaSubscriber: BaseFlowLogEventSubscriber() {

    abstract val events: Set<String>
    protected abstract val name: String
    protected abstract val contract: Contracts

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = createDescriptors()

    private fun createDescriptors(): Map<FlowChainId, FlowDescriptor> {
        return FlowChainId.values()
            .mapNotNull { chainId ->
                if (contract.deployments[chainId] != null)
                    chainId to createFlowNftMetaDescriptor(chainId)
                else null
            }
            .toMap()
    }

    private fun createFlowNftMetaDescriptor(chainId: FlowChainId): FlowDescriptor {
        return DescriptorFactory.flowNftOrderDescriptor(
            contract = contract,
            events = events,
            chainId = chainId,
            dbCollection = collection,
            name = name
        )
    }
}
