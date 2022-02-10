package com.rarible.flow.scanner.listener.activity

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.activitymaker.NFTActivityMaker
import org.springframework.stereotype.Component

@Component
class OneFootballActivity : NFTActivityMaker() {

    override val contractName: String = Contracts.ONE_FOOTBALL.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()
}
