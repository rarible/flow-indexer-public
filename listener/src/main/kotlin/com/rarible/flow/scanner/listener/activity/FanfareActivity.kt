package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.cadence.StringField
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.activitymaker.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties
import org.springframework.stereotype.Component

@Component
class FanfareActivity(
    private val config: FlowListenerProperties
): NFTActivityMaker() {
    override val contractName: String = Contracts.FANFARE.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val metadata: StringField by logEvent.event.fields

        return mapOf(
            "metadata" to metadata.value!!
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.FANFARE.staticRoyalties(config.chainId)
    }
}
