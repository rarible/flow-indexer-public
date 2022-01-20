package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class VersusArtSubscriber : BaseFlowLogEventSubscriber() {

    private val events = setOf("Created", "Withdraw", "Deposit")
    private val contractName = "Art"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "d796ff17107bbff6",
                contract = contractName,
                events = events,
                startFrom = 13939098L,
                dbCollection = collection
            ),
            FlowChainId.TESTNET to flowDescriptor(
                address = "99ca04281098b33d",
                events = events,
                contract = contractName,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                address = "f8d6e0586b0a20c7",
                events = events,
                contract = contractName,
                dbCollection = collection
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when (EventId.of(log.event.id).eventName) {
        "Created" -> FlowLogType.MINT
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        else -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
    }
}
