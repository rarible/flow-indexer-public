package com.rarible.flow.scanner.test

import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.FlowEventPayload
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.event.EventMessageCadenceConverter
import com.rarible.flow.core.test.randomFlowEvent
import com.rarible.flow.core.test.randomFlowLogEvent

abstract class BaseJsonEventTest {

    protected fun getFlowEvent(resource: String): FlowEvent {
        val json = this.javaClass
            .getResourceAsStream(resource)!!
            .bufferedReader().use { it.readText() }

        return randomFlowEvent().copy(payload = FlowEventPayload(json.toByteArray()))
    }

    protected fun getMintFlowLogEvent(): FlowLogEvent {
        return randomFlowLogEvent(event = getMintEventMessage())
    }

    protected fun getBurnFlowLogEvent(): FlowLogEvent {
        return randomFlowLogEvent(event = getBurnEventMessage())
    }

    protected fun getDepositFlowLogEvent(): FlowLogEvent {
        return randomFlowLogEvent(event = getDepositEventMessage())
    }

    protected fun getWithdrawFlowLogEvent(): FlowLogEvent {
        return randomFlowLogEvent(event = getWithdrawEventMessage())
    }

    protected fun getStorefrontV2ListingFlowLogEvent(): FlowLogEvent {
        return randomFlowLogEvent(event = getStorefrontV2ListingEventMessage())
    }

    protected fun getStorefrontV2PurchaseFlowLogEvent(): FlowLogEvent {
        return randomFlowLogEvent(event = getStorefrontV2PurchaseEventMessage())
    }

    protected fun getStorefrontV2PurchaseEventMessage(): EventMessage {
        return getEventMessage("/json/nft_storefront_v2_purchase.json")
    }

    protected fun getStorefrontV2ListingEventMessage(): EventMessage {
        return getEventMessage("/json/nft_storefront_v2_listing.json")
    }

    protected fun getWithdrawEventMessage(): EventMessage {
        return getEventMessage("/json/nft_withdraw.json")
    }

    protected fun getDepositEventMessage(): EventMessage {
        return getEventMessage("/json/nft_deposit.json")
    }

    protected fun getBurnEventMessage(): EventMessage {
        return getEventMessage("/json/nft_burn.json")
    }

    protected fun getMintEventMessage(): EventMessage {
        return getEventMessage("/json/nft_mint.json")
    }

    protected fun getEventMessage(resource: String): EventMessage {
        val event = getFlowEvent(resource)
        return EventMessageCadenceConverter.convert(event.event)
    }
}