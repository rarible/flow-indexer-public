package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.test.randomFlowLogEvent
import com.rarible.flow.scanner.service.CurrencyService
import com.rarible.flow.scanner.test.BaseJsonEventTest
import io.mockk.mockk

abstract class BaseNFTStorefrontEventParserTest : BaseJsonEventTest() {
    protected val currencyService = mockk<CurrencyService>()

    protected fun getFlowLogEvent(json: String, type: FlowLogType): FlowLogEvent {
        val flowEvent = getFlowEvent(json)
        val eventMessage = com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, flowEvent.event)
        val flowLogEvent = randomFlowLogEvent()
        val log = flowLogEvent.log.copy(transactionHash = "80eefea7f1ccf8c89accab5118eec4f0e9af88bb7373b5b258c0cab5c5c45192")
        return flowLogEvent.copy(
            event = eventMessage,
            type = type,
            log = log
        )
    }
}

