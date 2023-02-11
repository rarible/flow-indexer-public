package com.rarible.flow.scanner.listener.activity

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.activitymaker.NFTActivityMaker
import org.springframework.stereotype.Component

@Component
class DisruptArtActivityMaker: NFTActivityMaker() {
    override val contractName: String = "DisruptArt"

    override fun isSupportedCollection(collection: String): Boolean {
        return collection == Contracts.DISRUPT_ART.fqn(chainId)
    }

    override fun tokenId(logEvent: FlowLogEvent): Long = parse {
        long(logEvent.event.fields["id"]!!)
    }

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val res = parse {
            mutableMapOf(
                "content" to string(logEvent.event.fields["content"]!!),
                "name" to string(logEvent.event.fields["name"]!!)
            )
        }

        if (logEvent.event.eventId.eventName == "GroupMint") {
            res["tokenGroupId"] = parse { long(logEvent.event.fields["tokenGroupId"]!!) }.toString()
        }
        return res.toMap()
    }

    override fun creator(logEvent: FlowLogEvent): String = parse {
        optional(logEvent.event.fields["owner"]!!) {
            address(it)
        }
    } ?: super.creator(logEvent)

    override fun royalties(logEvent: FlowLogEvent): List<Part> = Contracts.DISRUPT_ART.staticRoyalties(chainId)
}
