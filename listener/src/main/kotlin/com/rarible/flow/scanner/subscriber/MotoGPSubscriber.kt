package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component
import java.util.*

@Component
class MotoGPSubscriber : BaseFlowLogEventSubscriber() {

    private val events = setOf("Mint", "Withdraw", "Deposit", "Burn")
    private val name = "moto_gp"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowNftDescriptor(
                contract = Contracts.MOTOGP,
                chainId = FlowChainId.MAINNET,
                events = events,
                startFrom = 16246182L,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.TESTNET to flowNftDescriptor(
                contract = Contracts.MOTOGP,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection,
                name = name,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType =
        FlowLogType.valueOf(EventId.of(log.event.id).eventName.uppercase(Locale.ENGLISH))
}
