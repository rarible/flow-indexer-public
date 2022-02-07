package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class RaribleNFTv2Subscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("Minted", "Withdraw", "Deposit", "Burned")
    private val contractName = "RaribleNFTv2"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.TESTNET to flowDescriptor(
                address = "ebf4ae01d1284af8",
                events = events,
                contract = contractName,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                address = "f8d6e0586b0a20c7",
                events = events,
                contract = contractName,
                dbCollection = collection
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.id).eventName) {
        "Minted" -> FlowLogType.MINT
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Burned" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
    }
}
