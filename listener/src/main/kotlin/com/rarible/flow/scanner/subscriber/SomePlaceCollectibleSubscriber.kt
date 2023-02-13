package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component

@Component
class SomePlaceCollectibleSubscriber: BaseFlowLogEventSubscriber() {

    private val events = listOf("Mint", "Deposit", "Withdraw", "Burn")
    private val name = "some_place_collection"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftDescriptor(
                contract = Contracts.SOME_PLACE_COLLECTIBLE,
                chainId = FlowChainId.MAINNET,
                events = events,
                startFrom = 25435115L,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowNftDescriptor(
                contract = Contracts.SOME_PLACE_COLLECTIBLE,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowNftDescriptor(
                contract = Contracts.SOME_PLACE_COLLECTIBLE,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection,
                name = name,
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Mint" -> FlowLogType.MINT
        "Deposit" -> FlowLogType.DEPOSIT
        "Withdraw" -> FlowLogType.WITHDRAW
        "Burn" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }


}
