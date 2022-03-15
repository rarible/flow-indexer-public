package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId

// TODO uncomment when legal is ready
//@Component
class TopShotSubscriber : BaseFlowLogEventSubscriber() {

    private val events = "Withdraw,Deposit,MomentMinted,MomentDestroyed".split(",")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = Contracts.TOPSHOT,
                chainId = FlowChainId.MAINNET,
                events = events,
                startFrom = 7641063L,
                dbCollection = collection
            ),
            FlowChainId.TESTNET to flowDescriptor(
                contract = Contracts.TOPSHOT,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = Contracts.TOPSHOT,
                chainId = FlowChainId.EMULATOR,
                events = events,
                startFrom = 1L,
                dbCollection = collection
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when (EventId.of(log.event.id).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "MomentMinted" -> FlowLogType.MINT
        "MomentDestroyed" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
    }
}
