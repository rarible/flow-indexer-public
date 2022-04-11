package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class JambbMomentsSubscriber : BaseFlowLogEventSubscriber() {
    val events = setOf("MomentMinted", "Withdraw", "Deposit", "MomentDestroyed")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = Contracts.JAMBB_MOMENTS,
                chainId =FlowChainId.MAINNET,
                events = events,
                dbCollection = collection,
                startFrom = 20445936,
            ),

            FlowChainId.TESTNET to flowDescriptor(
                contract = Contracts.JAMBB_MOMENTS,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection,
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
