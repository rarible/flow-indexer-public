package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.FlowEventPayload
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.test.randomFlowEvent
import com.rarible.flow.core.test.randomFlowLogEvent
import com.rarible.flow.scanner.service.CurrencyService
import io.mockk.mockk

abstract class BaseNFTStorefrontEventParserTest {
    protected val currencyService = mockk<CurrencyService>()

    protected fun getFlowLogEvent(json: String, type: FlowLogType): FlowLogEvent {
        val flowEvent = getFlowEvent(json)
        val eventMessage = com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, flowEvent.event)
        return randomFlowLogEvent().copy(event = eventMessage, type = type)
    }

    private fun getFlowEvent(resource: String): FlowEvent {
        val json = this.javaClass
            .getResourceAsStream(resource)!!
            .bufferedReader().use { it.readText() }

        return randomFlowEvent().copy(payload = FlowEventPayload(json.toByteArray()))
    }
}