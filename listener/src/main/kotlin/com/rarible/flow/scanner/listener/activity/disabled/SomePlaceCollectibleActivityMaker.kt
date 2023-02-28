package com.rarible.flow.scanner.listener.activity.disabled

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.activitymaker.NFTActivityMaker

class SomePlaceCollectibleActivityMaker: NFTActivityMaker() {
    override val contractName: String
        get() = Contracts.SOME_PLACE_COLLECTIBLE.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override fun royalties(logEvent: FlowLogEvent): List<Part> =
        Contracts.SOME_PLACE_COLLECTIBLE.staticRoyalties(chainId)

    override fun isSupportedCollection(collection: String): Boolean =
        collection == Contracts.SOME_PLACE_COLLECTIBLE.fqn(chainId)
}
