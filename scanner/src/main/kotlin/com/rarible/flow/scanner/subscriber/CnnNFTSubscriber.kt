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
class CnnNFTSubscriber : BaseFlowLogEventSubscriber() {
    val events = setOf("Minted", "Withdraw", "Deposit", "NFTDestroyed")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                contract = "CNN_NFT",
                address = "329feb3ab062d289",
                events = events,
                dbCollection = collection,
                startFrom = 15640000L,
            ),
            FlowChainId.TESTNET to flowDescriptor(
                contract = "CNN_NFT",
                address = "ebf4ae01d1284af8",
                events = events,
                dbCollection = collection,
                startFrom = 53489946L
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                contract = "CNN_NFT",
                address = "f8d6e0586b0a20c7",
                events = events,
                dbCollection = collection,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Mint" -> FlowLogType.MINT
        "NFTDestroyed" -> FlowLogType.BURN
        else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
