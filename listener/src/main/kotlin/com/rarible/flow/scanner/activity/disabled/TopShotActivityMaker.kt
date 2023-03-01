package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.cadence.NumberField
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.activity.NFTActivityMaker

class TopShotActivityMaker : NFTActivityMaker() {
    override val contractName: String = Contracts.TOPSHOT.contractName

    override fun isSupportedCollection(collection: String): Boolean {
        return collection == Contracts.TOPSHOT.fqn(chainId)
    }

    override fun tokenId(logEvent: FlowLogEvent): Long = when (logEvent.type) {
        FlowLogType.MINT -> cadenceParser.long(logEvent.event.fields["momentID"]!!)
        else -> cadenceParser.long(logEvent.event.fields["id"]!!)
    }

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val msg = logEvent.event
        val playID: NumberField by msg.fields
        val setID: NumberField by msg.fields
        val serialNumber: NumberField by msg.fields
        return mapOf(
            "playID" to playID.value.toString(),
            "setID" to setID.value.toString(),
            "serialNumber" to serialNumber.value.toString()
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.TOPSHOT.staticRoyalties(chainId)
    }
}
