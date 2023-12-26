package com.rarible.flow.scanner.subscriber.nft.disabled

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.subscriber.BaseFlowLogEventSubscriber
import com.rarible.flow.scanner.subscriber.DescriptorFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class MatrixWorldFlowFestSubscriber(chainId: FlowChainId) : BaseFlowLogEventSubscriber(chainId) {

    private val events = setOf("Minted", "Withdraw", "Deposit")
    private val name = "matrix_world_flow_fest"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.MATRIX_WORLD_FLOW_FEST.contractName,
                address = Contracts.MATRIX_WORLD_FLOW_FEST.deployments[FlowChainId.MAINNET]!!.base16Value,
                events = events,
                dbCollection = collection,
                startFrom = 19004982L,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.MATRIX_WORLD_FLOW_FEST.contractName,
                address = Contracts.MATRIX_WORLD_FLOW_FEST.deployments[FlowChainId.TESTNET]!!.base16Value,
                events = events,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.MATRIX_WORLD_FLOW_FEST.contractName,
                address = Contracts.MATRIX_WORLD_FLOW_FEST.deployments[FlowChainId.EMULATOR]!!.base16Value,
                events = events,
                dbCollection = collection,
                name = name,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when (EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Minted" -> FlowLogType.MINT
        else -> throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
