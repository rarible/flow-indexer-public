package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class SoftCollectionSubscriber: BaseFlowLogEventSubscriber() {

    private val contractName = "SoftCollection"

    private val events = setOf("Withdraw", "Deposit", "Minted", "Burned", "Changed")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "01ab36aaf654a13e",
                contract = contractName,
                events = events,
                startFrom = 19799019L,
                dbCollection = collection
            ),
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

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.COLLECTION_WITHDRAW
        "Deposit" -> FlowLogType.COLLECTION_DEPOSIT
        "Minted" -> FlowLogType.COLLECTION_MINT
        "Burned" -> FlowLogType.COLLECTION_BURN
        "Changed" -> FlowLogType.COLLECTION_CHANGE
        else -> throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
