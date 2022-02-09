package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["blockchain.scanner.flow.chainId"], havingValue = "MAINNET")
class DisruptArtSubscriber: BaseFlowLogEventSubscriber() {

    private val contractName = "DisruptArt"

    private val events = setOf("Mint", "Withdraw", "Deposit", "GroupMint")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "cd946ef9b13804c6",
                contract = contractName,
                events = events,
                dbCollection = collection,
                startFrom = 19100120L
            ),
            FlowChainId.TESTNET to flowDescriptor(
                address = "439c2b49c0b2f62b",
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
            "Mint", "GroupMint" -> FlowLogType.MINT
            else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
        }
}
