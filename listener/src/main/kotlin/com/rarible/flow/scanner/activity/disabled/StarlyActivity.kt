package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.cadence.StringField
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker

class StarlyActivity(
    private val config: FlowListenerProperties
): NFTActivityMaker() {
    override val contractName: String = Contracts.STARLY_CARD.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val starlyID: StringField by logEvent.event.fields
        return mapOf(
            "starlyId" to starlyID.value!!,
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.STARLY_CARD.staticRoyalties(config.chainId)
    }
}
