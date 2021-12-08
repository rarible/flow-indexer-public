package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class TopShotSubscriber : BaseFlowLogEventSubscriber() {

    private val events = "Withdraw,Deposit,MomentMinted,MomentDestroyed".split(",")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "0b2a3299cc857e29",
                contract = "TopShot",
                events = events,
                startFrom = 7641063L
            ),
            FlowChainId.TESTNET to flowDescriptor(
                address = "01658d9b94068f3c",
                contract = "TopShot",
                events = events,
                startFrom = 47831085L
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                address = "f8d6e0586b0a20c7",
                contract = "TopShot",
                events = events,
                startFrom = 1L
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.id).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "MomentMinted" -> FlowLogType.MINT
        "MomentDestroyed" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
    }
}
