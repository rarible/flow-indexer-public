package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.cadence.UInt32NumberField
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.activitymaker.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowApiProperties
import org.springframework.stereotype.Component

@Component
class ChainmonstersActivity(
    private val config: FlowApiProperties
): NFTActivityMaker() {
    override val contractName: String = Contracts.JAMBB_MOMENTS.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["NFTID"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val rewardID: UInt32NumberField by logEvent.event.fields

        return mapOf(
            "rewardId" to rewardID.value!!,
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.JAMBB_MOMENTS.staticRoyalties(config.chainId)
    }
}