package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["blockchain.scanner.flow.chainId"], havingValue = "MAINNET")
class DisruptArtSubscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("Mint", "Withdraw", "Deposit", "GroupMint")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = Contracts.DISRUPT_ART,
                chainId = FlowChainId.MAINNET,
                events = events,
                dbCollection = collection,
                startFrom = 19100120L
            ),
            FlowChainId.TESTNET to flowDescriptor(
                contract = Contracts.DISRUPT_ART,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = Contracts.DISRUPT_ART,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType =
        when(EventId.of(log.event.type).eventName) {
            "Withdraw" -> FlowLogType.WITHDRAW
            "Deposit" -> FlowLogType.DEPOSIT
            "Mint", "GroupMint" -> FlowLogType.MINT
            else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
        }
}
