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
class JambbMomentsSubscriber : BaseFlowLogEventSubscriber() {

    private val events = setOf("MomentMinted", "Withdraw", "Deposit", "MomentDestroyed")
    private val name = "jambb_moments"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.JAMBB_MOMENTS,
                chainId = FlowChainId.MAINNET,
                events = events,
                dbCollection = collection,
                startFrom = 20445936,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.JAMBB_MOMENTS,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection,
                name = name,
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "MomentMinted" -> FlowLogType.MINT
        "MomentDestroyed" -> FlowLogType.BURN
        else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
