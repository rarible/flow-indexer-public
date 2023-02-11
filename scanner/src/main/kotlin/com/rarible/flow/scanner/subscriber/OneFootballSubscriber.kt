package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component

@Component
class OneFootballSubscriber : BaseFlowLogEventSubscriber() {
    val events = setOf("Minted", "Withdraw", "Deposit", "Destroyed")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowNftDescriptor(
                contract = Contracts.ONE_FOOTBALL.contractName,
                address = Contracts.ONE_FOOTBALL.deployments[FlowChainId.MAINNET]!!.base16Value,
                events = events,
                dbCollection = collection,
                startFrom = 21831983L,
            ),
            FlowChainId.TESTNET to flowNftDescriptor(
                contract = Contracts.ONE_FOOTBALL.contractName,
                address = Contracts.ONE_FOOTBALL.deployments[FlowChainId.TESTNET]!!.base16Value,
                events = events,
                dbCollection = collection,
            ),
            FlowChainId.EMULATOR to flowNftDescriptor(
                contract = Contracts.ONE_FOOTBALL.contractName,
                address = Contracts.ONE_FOOTBALL.deployments[FlowChainId.EMULATOR]!!.base16Value,
                events = events,
                dbCollection = collection,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Minted" -> FlowLogType.MINT
        "Destroyed" -> FlowLogType.BURN
        else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
