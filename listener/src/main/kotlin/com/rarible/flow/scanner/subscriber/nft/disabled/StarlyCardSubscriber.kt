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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ExperimentalCoroutinesApi
@ConditionalOnProperty(name = ["blockchain.scanner.flow.chainId"], havingValue = "MAINNET")
class StarlyCardSubscriber(chainId: FlowChainId) : BaseFlowLogEventSubscriber(chainId) {

    val events = setOf("Minted", "Withdraw", "Deposit", "Burned")
    private val name = "starly_card"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftOrderDescriptor(
                contract = Contracts.STARLY_CARD.contractName,
                address = Contracts.STARLY_CARD.deployments[FlowChainId.MAINNET]!!.base16Value,
                events = events,
                dbCollection = collection,
                startFrom = 18133134L,
                name = name
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when (EventId.of(log.event.type).eventName) {
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Minted" -> FlowLogType.MINT
        "Burned" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.type}")
    }
}
