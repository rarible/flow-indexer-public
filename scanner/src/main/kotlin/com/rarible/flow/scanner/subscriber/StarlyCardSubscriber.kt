package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class StarlyCardSubscriber : BaseFlowLogEventSubscriber() {
    val events = setOf("Minted", "Withdraw", "Deposit", "Burned")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = "StarlyCard",
                address = "5b82f21c0edf76e3",
                events = events,
                dbCollection = collection,
                startFrom = 19040960L,
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Minted" -> FlowLogType.MINT
        "Burned" -> FlowLogType.BURN
        else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
