package com.rarible.flow.scanner.subscriber.nft.disabled

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.subscriber.BaseFlowLogEventSubscriber
import com.rarible.flow.scanner.subscriber.DescriptorFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression

@ConditionalOnExpression("false")
class EnglishAuctionSubscriber : BaseFlowLogEventSubscriber() {

    private val contractName = Contracts.ENGLISH_AUCTION.contractName

    private val events = setOf("LotAvailable", "LotCompleted", "LotEndTimeChanged", "LotCleaned", "OpenBid", "CloseBid", "IncreaseBid")
    private val name = "english_auction"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.TESTNET to DescriptorFactory.flowAuctionDescriptor(
                address = "ebf4ae01d1284af8",
                contract = contractName,
                events = events,
                dbCollection = collection,
                name = name,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when (EventId.of(log.event.type).eventName) {
        "LotAvailable" -> FlowLogType.LOT_AVAILABLE
        "LotCompleted" -> FlowLogType.LOT_COMPLETED
        "LotEndTimeChanged" -> FlowLogType.LOT_END_TIME_CHANGED
        "LotCleaned" -> FlowLogType.LOT_CLEANED
        "OpenBid" -> FlowLogType.OPEN_BID
        "CloseBid" -> FlowLogType.CLOSE_BID
        "IncreaseBid" -> FlowLogType.INCREASE_BID
        else -> throw IllegalStateException("Unsupported event type [${log.event.type}]")
    }
}
