package com.rarible.flow.scanner.subscriber.nft.disabled

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.subscriber.BaseFlowLogEventSubscriber
import com.rarible.flow.scanner.subscriber.DescriptorFactory

class SoftCollectionSubscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("Withdraw", "Deposit", "Minted", "Burned", "Changed")
    private val name = "soft_collection"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowCollectionDescriptor(
                contract = Contracts.SOFT_COLLECTION,
                chainId = FlowChainId.MAINNET,
                events = events,
                startFrom = 19799019L,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowCollectionDescriptor(
                contract = Contracts.SOFT_COLLECTION,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowCollectionDescriptor(
                contract = Contracts.SOFT_COLLECTION,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection,
                name = name,
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.COLLECTION_WITHDRAW
        "Deposit" -> FlowLogType.COLLECTION_DEPOSIT
        "Minted" -> FlowLogType.COLLECTION_MINT
        "Burned" -> FlowLogType.COLLECTION_BURN
        "Changed" -> FlowLogType.COLLECTION_CHANGE
        else -> throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
