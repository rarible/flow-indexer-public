package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component

@Component
class ChainMonstersSubscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("NFTMinted", "Withdraw", "Deposit")
    private val name = "chain_monsters"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftDescriptor(
                contract = Contracts.CHAINMONSTERS,
                chainId = FlowChainId.MAINNET,
                events = events,
                dbCollection = collection,
                startFrom = 11283560L,
                name = name
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowNftDescriptor(
                contract = Contracts.CHAINMONSTERS,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection,
                name = name
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowNftDescriptor(
                contract = Contracts.CHAINMONSTERS,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection,
                name = name
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType =
        when(EventId.of(log.event.type).eventName) {
            "Withdraw" -> FlowLogType.WITHDRAW
            "Deposit" -> FlowLogType.DEPOSIT
            "NFTMinted" -> FlowLogType.MINT
            else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
        }
}
