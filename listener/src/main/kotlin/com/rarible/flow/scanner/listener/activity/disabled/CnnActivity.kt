package com.rarible.flow.scanner.listener.activity.disabled

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.activitymaker.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties

class CnnActivity(
    private val config: FlowListenerProperties
) : NFTActivityMaker() {

    override val contractName: String = Contracts.CNN.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.CNN.staticRoyalties(config.chainId)
    }
}
