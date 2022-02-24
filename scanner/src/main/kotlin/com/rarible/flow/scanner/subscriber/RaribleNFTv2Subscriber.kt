package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class RaribleNFTv2Subscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("Minted", "Withdraw", "Deposit", "Burned")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.TESTNET to flowDescriptor(
                contract = Contracts.RARIBLE_NFTV2,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = Contracts.RARIBLE_NFTV2,
                chainId = FlowChainId.EMULATOR,
                events = events,
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
