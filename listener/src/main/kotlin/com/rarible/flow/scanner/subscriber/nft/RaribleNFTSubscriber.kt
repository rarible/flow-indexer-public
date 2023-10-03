package com.rarible.flow.scanner.subscriber.nft

import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.model.RaribleNftEventType
import com.rarible.flow.scanner.subscriber.EnableRaribleNft
import org.springframework.stereotype.Component

@Component
@EnableRaribleNft
class RaribleNFTSubscriber : NonFungibleTokenSubscriber() {
    override val events = RaribleNftEventType.EVENT_NAMES
    override val name = "rarible_nft"
    override val contract = Contracts.RARIBLE_NFT

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType {
        val eventType = RaribleNftEventType.fromEventName(
            EventId.of(log.event.id).eventName
        )
        return when (eventType) {
            RaribleNftEventType.WITHDRAW -> FlowLogType.WITHDRAW
            RaribleNftEventType.DEPOSIT -> FlowLogType.DEPOSIT
            RaribleNftEventType.MINT -> FlowLogType.MINT
            RaribleNftEventType.BURN -> FlowLogType.BURN
            null -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
        }
    }
}
