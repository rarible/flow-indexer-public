package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class MugenNFTSubscriber : BaseFlowLogEventSubscriber() {

    private val events = setOf("Minted", "Withdraw", "Deposit")
    private val name = "mugen_nft"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowNftDescriptor(
                contract = "MugenNFT",
                address = "2cd46d41da4ce262",
                events = events,
                dbCollection = collection,
                startFrom = 19040960L,
                name = name
            ),
            FlowChainId.TESTNET to flowNftDescriptor(
                contract = "MugenNFT",
                address = "ebf4ae01d1284af8",
                events = events,
                dbCollection = collection,
                name = name
            ),
            FlowChainId.EMULATOR to flowNftDescriptor(
                contract = "MugenNFT",
                address = "f8d6e0586b0a20c7",
                events = events,
                dbCollection = collection,
                name = name
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Minted" -> FlowLogType.MINT
        else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
