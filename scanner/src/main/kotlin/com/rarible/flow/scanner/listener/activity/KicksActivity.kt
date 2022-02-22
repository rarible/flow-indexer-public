package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.cadence.UInt32NumberField
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.config.FlowApiProperties
import com.rarible.flow.scanner.listener.NFTActivityMaker
import org.springframework.stereotype.Component

@Component
class KicksActivity(
    private val config: FlowApiProperties
): NFTActivityMaker() {
    override val contractName: String = Contracts.KICKS.contractName

    override fun isSupportedCollection(collection: String): Boolean {
        return collection == Contracts.KICKS.fqn(config.chainId)
    }

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val setID: UInt32NumberField by logEvent.event.fields
        val blueprintID: UInt32NumberField by logEvent.event.fields
        val instanceID: UInt32NumberField by logEvent.event.fields

        return mapOf(
            "setID" to setID.value!!,
            "blueprintID" to blueprintID.value!!,
            "instanceID" to instanceID.value!!
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.KICKS.staticRoyalties(config.chainId)
    }
}