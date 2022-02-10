package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class EvolutionSubscriber : BaseFlowLogEventSubscriber() {

    private val events = "Withdraw,Deposit,CollectibleMinted,CollectibleDestroyed".split(",")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = Contracts.EVOLUTION,
                chainId = FlowChainId.MAINNET,
                events = events,
                startFrom = 13001301L,
                dbCollection = collection,
            ),
            FlowChainId.TESTNET to flowDescriptor(
                contract = Contracts.EVOLUTION,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = Contracts.EVOLUTION,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection,
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "CollectibleMinted" -> FlowLogType.MINT
        "CollectibleDestroyed" -> FlowLogType.BURN
        else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
