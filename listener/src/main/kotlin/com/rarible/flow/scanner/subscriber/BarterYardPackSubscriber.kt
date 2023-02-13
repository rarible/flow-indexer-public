package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component

@Component
class BarterYardPackSubscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("Mint", "Withdraw", "Deposit", "Burn")

    override val descriptors: Map<FlowChainId, FlowDescriptor> = mapOf(
        FlowChainId.MAINNET to DescriptorFactory.flowNftDescriptor(
            contract = Contracts.BARTER_YARD_PACK,
            chainId = FlowChainId.MAINNET,
            events = events,
            startFrom = 24184883L,
            dbCollection = collection,
            name = DESCRIPTOR_NAME
        ),
        FlowChainId.TESTNET to DescriptorFactory.flowNftDescriptor(
            contract = Contracts.BARTER_YARD_PACK,
            chainId = FlowChainId.TESTNET,
            events = events,
            dbCollection = collection,
            name = DESCRIPTOR_NAME
        )
    )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Mint" -> FlowLogType.MINT
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Burn" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.type}!")
    }

    private companion object {
        const val DESCRIPTOR_NAME = "barter_yard_pack"
    }
}
