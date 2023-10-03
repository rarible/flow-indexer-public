package com.rarible.flow.scanner.subscriber.nft.disabled

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.subscriber.BaseFlowLogEventSubscriber
import com.rarible.flow.scanner.subscriber.DescriptorFactory

class OneFootballSubscriber : BaseFlowLogEventSubscriber() {
    private val events = setOf("Minted", "Withdraw", "Deposit", "Destroyed")
    private val name = "one_football"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.ONE_FOOTBALL.contractName,
                address = Contracts.ONE_FOOTBALL.deployments[FlowChainId.MAINNET]!!.base16Value,
                events = events,
                dbCollection = collection,
                startFrom = 21831983L,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.ONE_FOOTBALL.contractName,
                address = Contracts.ONE_FOOTBALL.deployments[FlowChainId.TESTNET]!!.base16Value,
                events = events,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.ONE_FOOTBALL.contractName,
                address = Contracts.ONE_FOOTBALL.deployments[FlowChainId.EMULATOR]!!.base16Value,
                events = events,
                dbCollection = collection,
                name = name,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when (EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Minted" -> FlowLogType.MINT
        "Destroyed" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
