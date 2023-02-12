package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component

@Component
class GeniaceSubscriber : BaseFlowLogEventSubscriber() {

    private val events = mapOf(
        "Minted" to FlowLogType.MINT,
        "Deposit" to FlowLogType.DEPOSIT,
        "Withdraw" to FlowLogType.WITHDRAW,
    )
    private val name = "geniace"

    override val descriptors: Map<FlowChainId, FlowDescriptor> = mapOf(
        FlowChainId.MAINNET to flowNftDescriptor(
            contract = Contracts.GENIACE,
            chainId = FlowChainId.MAINNET,
            events = events.keys,
            startFrom = 21582165L,
            dbCollection = collection,
            name = name,
        ),
        FlowChainId.TESTNET to flowNftDescriptor(
            contract = Contracts.GENIACE,
            chainId = FlowChainId.TESTNET,
            events = events.keys,
            dbCollection = collection,
            name = name,
        ),
        FlowChainId.EMULATOR to flowNftDescriptor(
            contract = Contracts.GENIACE,
            chainId = FlowChainId.EMULATOR,
            events = events.keys,
            dbCollection = collection,
            name = name,
        ),
    )

    override suspend fun eventType(log: FlowBlockchainLog) =
        events[EventId.of(log.event.type).eventName]
            ?: throw IllegalStateException("Unsupported event type: ${log.event.type}")
}
