package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component
import java.util.*

@Component
class MotoGPSubscriber : BaseFlowLogEventSubscriber() {

    private val events = setOf("Mint", "Withdraw", "Deposit", "Burn")

    private val contractName = "MotoGPCard"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "a49cc0ee46c54bfb",
                contract = contractName,
                events = events,
                startFrom = 16246182L
            ),
            FlowChainId.TESTNET to flowDescriptor(
                address = "01658d9b94068f3c",
                contract = contractName,
                events = events,
                startFrom = 47831085L
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType =
        FlowLogType.valueOf(EventId.of(log.event.id).eventName.uppercase(Locale.ENGLISH))
}
