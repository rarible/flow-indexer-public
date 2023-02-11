package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component

@Component
class SomePlaceCollectibleSubscriber: BaseFlowLogEventSubscriber() {

    private val events = listOf("Mint", "Deposit", "Withdraw", "Burn")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = Contracts.SOME_PLACE_COLLECTIBLE,
                chainId = FlowChainId.MAINNET,
                events = events,
                startFrom = 25435115L,
                dbCollection = collection
            ),
            FlowChainId.TESTNET to flowDescriptor(
                contract = Contracts.SOME_PLACE_COLLECTIBLE,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = Contracts.SOME_PLACE_COLLECTIBLE,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Mint" -> FlowLogType.MINT
        "Deposit" -> FlowLogType.DEPOSIT
        "Withdraw" -> FlowLogType.WITHDRAW
        "Burn" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }


}
