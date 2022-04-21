package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.listener.NFTActivityMaker
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DisruptArtActivityMaker(
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId,
) : NFTActivityMaker() {
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