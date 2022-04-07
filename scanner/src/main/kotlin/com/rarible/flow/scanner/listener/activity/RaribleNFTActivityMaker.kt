package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.listener.NFTActivityMaker
import org.springframework.stereotype.Component

@Component
class RaribleNFTActivityMaker : NFTActivityMaker() {
    override val contractName: String = "RaribleNFT"

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        return try {
            cadenceParser.dictionaryMap(logEvent.event.fields["metadata"]!!) { key, value ->
                string(key) to string(value)
            }
        } catch (_: Exception) {
            mapOf("metaURI" to cadenceParser.string(logEvent.event.fields["metadata"]!!))
        }
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return cadenceParser.arrayValues(logEvent.event.fields["royalties"]!!) {
            it as StructField
            Part(
                address = FlowAddress(address(it.value!!.getRequiredField("address"))),
                fee = double(it.value!!.getRequiredField("fee"))
            )
        }
    }

    override fun creator(logEvent: FlowLogEvent): String {
        return cadenceParser.address(logEvent.event.fields["creator"]!!)
    }
}
