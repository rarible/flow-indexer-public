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
class MatrixWorldFlowFestSubscriber : BaseFlowLogEventSubscriber() {
    val events = setOf("Minted", "Withdraw", "Deposit")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = "MatrixWorldSubscriber",
                address = "2d2750f240198f91",
                events = events,
                dbCollection = collection,
                startFrom = 19040960L,
            ),
            FlowChainId.TESTNET to flowDescriptor(
                contract = "MatrixWorldSubscriber",
                address = "e2f1b000e0203c1d",
                events = events,
                dbCollection = collection,
                startFrom = 53489946L
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = "MatrixWorldSubscriber",
                address = "f8d6e0586b0a20c7",
                events = events,
                dbCollection = collection,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Minted" -> FlowLogType.MINT
        else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
