package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component

@Component
class VersusArtSubscriber : BaseFlowLogEventSubscriber() {

    private val events = setOf("Created", "Withdraw", "Deposit")
    private val additionalEvents = setOf(
        "Versus.Settle",
        "Versus.DropDestroyed",
    )
    private val name = "versus_art"

    override val descriptors: Map<FlowChainId, FlowDescriptor> = mapOf(
        FlowChainId.MAINNET to DescriptorFactory.flowVersusArtDescriptor(
            contract = Contracts.VERSUS_ART,
            chainId = FlowChainId.MAINNET,
            events = events,
            dbCollection = collection,
            startFrom = 13965155,
            additionalEvents = additionalEvents,
            name = name,
        ),
        FlowChainId.TESTNET to DescriptorFactory.flowVersusArtDescriptor(
            contract = Contracts.VERSUS_ART,
            chainId = FlowChainId.TESTNET,
            events = events,
            dbCollection = collection,
            additionalEvents = additionalEvents,
            name = name,
        ),
        FlowChainId.EMULATOR to DescriptorFactory.flowVersusArtDescriptor(
            contract = Contracts.VERSUS_ART,
            chainId = FlowChainId.EMULATOR,
            events = events,
            dbCollection = collection,
            additionalEvents = additionalEvents,
            name = name,
        ),
    )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when (EventId.of(log.event.id).eventName) {
        "Created" -> FlowLogType.MINT
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Settle", "DropDestroyed" -> FlowLogType.CUSTOM
        else -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
    }
}
