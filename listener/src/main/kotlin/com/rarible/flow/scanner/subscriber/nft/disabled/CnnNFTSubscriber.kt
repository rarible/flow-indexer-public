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
class CnnNFTSubscriber : BaseFlowLogEventSubscriber() {

    val events = setOf("Minted", "Withdraw", "Deposit", "NFTDestroyed")
    private val name = "cnn_nft"


    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() =  mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.CNN,
                chainId = FlowChainId.MAINNET,
                events = events,
                dbCollection = collection,
                startFrom = 15640000L,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.CNN,
                chainId = FlowChainId.TESTNET,
                events = events,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.CNN,
                chainId = FlowChainId.EMULATOR,
                events = events,
                dbCollection = collection,
                name = name,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Minted" -> FlowLogType.MINT
        "NFTDestroyed" -> FlowLogType.BURN
        else ->  throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
