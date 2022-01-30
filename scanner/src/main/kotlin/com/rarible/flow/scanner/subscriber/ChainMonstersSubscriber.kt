package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class ChainMonstersSubscriber: BaseFlowLogEventSubscriber() {

    private val contractName = "ChainmonstersRewards"

    private val events = setOf("NFTMinted", "Withdraw", "Deposit")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "93615d25d14fa337",
                contract = contractName,
                events = events,
                dbCollection = collection,
                startFrom = 19100120L
            ),
            FlowChainId.TESTNET to flowDescriptor(
                address = "75783e3c937304a8",
                contract = contractName,
                events = events,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = contractName,
                address = "f8d6e0586b0a20c7",
                events = events,
                dbCollection = collection,
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
