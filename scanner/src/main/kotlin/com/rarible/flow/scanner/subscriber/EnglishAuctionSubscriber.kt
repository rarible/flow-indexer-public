package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class EnglishAuctionSubscriber: BaseFlowLogEventSubscriber() {

    private val contractName = "EnglishAuction"

    private val events = setOf("LotAvailable", "LotCompleted", "LotEndTimeChanged", "LotCleaned", "OpenBid", "CloseBid")

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.TESTNET to flowDescriptor(
                address = "ebf4ae01d1284af8",
                contract = contractName,
                events = events,
                startFrom = 55102390L,
                dbCollection = collection
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.type).eventName) {
        "LotAvailable" -> FlowLogType.LOT_AVAILABLE
        "LotCompleted" -> FlowLogType.LOT_COMPLETED
        "LotEndTimeChanged" -> FlowLogType.LOT_END_TIME_CHANGED
        "LotCleaned" -> FlowLogType.LOT_CLEANED
        "OpenBid" -> FlowLogType.OPEN_BID
        "CloseBid" -> FlowLogType.CLOSE_BID
        else -> throw IllegalStateException("Unsupported event type [${log.event.type}]")
    }
}
