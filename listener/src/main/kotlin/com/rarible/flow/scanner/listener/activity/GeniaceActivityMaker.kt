package com.rarible.flow.scanner.listener.activity

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.activitymaker.NFTActivityMaker
import org.springframework.stereotype.Component

@Component
class GeniaceActivityMaker : NFTActivityMaker() {
    override val contractName = Contracts.GENIACE.contractName

    override fun tokenId(logEvent: FlowLogEvent) =
        cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()
}
