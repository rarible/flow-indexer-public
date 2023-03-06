package com.rarible.flow.scanner

import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.FlowEventPayload
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.event.EventMessageCadenceConverter
import com.rarible.flow.core.test.randomFlowEvent

abstract class BaseJsonEventTest {

    protected fun getFlowEvent(resource: String): FlowEvent {
        val json = this.javaClass
            .getResourceAsStream(resource)!!
            .bufferedReader().use { it.readText() }

        return randomFlowEvent().copy(payload = FlowEventPayload(json.toByteArray()))
    }

    protected fun getEventMessage(resource: String): EventMessage {
        val event = getFlowEvent(resource)
        return EventMessageCadenceConverter.convert(event.event)
    }
}