package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component

@Component
class KicksSubscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("SneakerCreated", "SneakerBurned", "Withdraw", "Deposit")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = Contracts.KICKS,
                chainId = FlowChainId.MAINNET,
                events = events,
                dbCollection = collection,
                startFrom = 21375610L
            ),
            FlowChainId.TESTNET to flowDescriptor(
                contract = Contracts.KICKS,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = Contracts.KICKS,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType =
        when(EventId.of(log.event.type).eventName) {
            "Withdraw" -> FlowLogType.WITHDRAW
            "Deposit" -> FlowLogType.DEPOSIT
            "SneakerCreated" -> FlowLogType.MINT
            "SneakerBurned" -> FlowLogType.BURN
            else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
        }
}
