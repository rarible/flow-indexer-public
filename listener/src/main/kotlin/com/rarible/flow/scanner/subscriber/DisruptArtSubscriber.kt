package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["blockchain.scanner.flow.chainId"], havingValue = "MAINNET")
class DisruptArtSubscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("Mint", "Withdraw", "Deposit", "GroupMint")
    private val name = "disrupt_art"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.DISRUPT_ART,
                chainId = FlowChainId.MAINNET,
                events = events,
                dbCollection = collection,
                startFrom = 19100120L,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.DISRUPT_ART,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.DISRUPT_ART,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection,
                name = name,
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
