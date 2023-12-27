package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker

class DisruptArtActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : NFTActivityMaker(flowLogRepository, txManager, chainId) {

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
