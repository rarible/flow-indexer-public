package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.model.NonFungibleTokenEventType

abstract class NonFungibleTokenSubscriber: BaseFlowLogEventSubscriber() {
    protected open val events = NonFungibleTokenEventType.EVENT_NAMES

    protected abstract val name: String
    protected abstract val contract: Contracts

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = contract,
                chainId = FlowChainId.MAINNET,
                events = events,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = contract,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowNftOrderDescriptor(
                contract = contract,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection,
                name = name,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType {
        val eventType = NonFungibleTokenEventType.fromEventName(
            EventId.of(log.event.id).eventName
        )
        return when (eventType) {
            NonFungibleTokenEventType.WITHDRAW -> FlowLogType.WITHDRAW
            NonFungibleTokenEventType.DEPOSIT -> FlowLogType.DEPOSIT
            NonFungibleTokenEventType.MINT -> FlowLogType.MINT
            NonFungibleTokenEventType.BURN -> FlowLogType.BURN
            null -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
        }
    }
}
