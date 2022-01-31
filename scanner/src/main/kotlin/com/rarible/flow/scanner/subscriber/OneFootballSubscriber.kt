package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class OneFootballSubscriber : BaseFlowLogEventSubscriber() {
    val events = setOf("Minted", "Withdraw", "Deposit", "Destroyed")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = "OneFootballCollectible",
                address = "6831760534292098",
                events = events,
                dbCollection = collection,
                startFrom = 21831957L,
            ),
            FlowChainId.TESTNET to flowDescriptor(
                contract = "OneFootballCollectible",
                address = "01984fb4ca279d9a",
                events = events,
                dbCollection = collection,
                startFrom = 53489946L
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = "OneFootballCollectible",
                address = "f8d6e0586b0a20c7",
                events = events,
                dbCollection = collection,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Minted" -> FlowLogType.MINT
        "Destroyed" -> FlowLogType.BURN
        else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
