package com.rarible.flow.scanner.listener.activity.disabled

import com.nftco.flow.sdk.cadence.UInt32NumberField
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.activitymaker.NFTActivityMaker

class ChainmonstersActivity: NFTActivityMaker() {
    override val contractName: String = Contracts.CHAINMONSTERS.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(
        logEvent.event.fields["NFTID"] ?: logEvent.event.fields["id"]!!
    )

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val rewardID: UInt32NumberField by logEvent.event.fields
        val serialNumber: UInt32NumberField by logEvent.event.fields

        return mapOf(
            "rewardId" to rewardID.value!!,
            "serialNumber" to serialNumber.value!!
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.CHAINMONSTERS.staticRoyalties(chainId)
    }
}
