package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class EvolutionSubscriber : BaseFlowLogEventSubscriber() {

    private val events = "Withdraw,Deposit,CollectibleMinted,CollectibleDestroyed".split(",")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "f4264ac8f3256818",
                contract = "Evolution",
                events = events,
                startFrom = 13001301L,
            ),
            FlowChainId.TESTNET to flowDescriptor(
                address = "01658d9b94068f3c",
                contract = "Evolution",
                events = events,
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                address = "f8d6e0586b0a20c7",
                contract = "Evolution",
                events = events,
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
