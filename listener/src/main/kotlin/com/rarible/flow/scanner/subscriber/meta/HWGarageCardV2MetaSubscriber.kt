package com.rarible.flow.scanner.subscriber.meta

import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.model.AdminMintCardEventType
import org.springframework.stereotype.Component

@Component
class HWGarageCardV2MetaSubscriber: AbstractNFTMetaSubscriber() {
    override val name = "hw_meta_card_v2"
    override val contract = Contracts.HW_GARAGE_PM_V2
    override val events = AdminMintCardEventType.EVENT_NAMES

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType {
        val eventType = AdminMintCardEventType.fromEventName(
            EventId.of(log.event.id).eventName
        )
        return when (eventType) {
            AdminMintCardEventType.ADMIN_MINT_CARD -> FlowLogType.ADMIN_MINT_CARD
            null -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
        }
    }
}


