package com.rarible.flow.scanner.listener.activity.disabled

import com.nftco.flow.sdk.Flow
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.event.VersusArtMetadata
import com.rarible.flow.scanner.activitymaker.NFTActivityMaker

class VersusArtActivityMaker : NFTActivityMaker() {

    override val contractName = Contracts.VERSUS_ART.contractName

    override fun tokenId(logEvent: FlowLogEvent) = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent) = try {
        val meta = Flow.unmarshall(VersusArtMetadata::class, logEvent.event.fields["metadata"]!!)
        mapOf(
            "name" to meta.name,
            "artist" to meta.artist,
            "artistAddress" to meta.artistAddress,
            "description" to meta.description,
            "type" to meta.type,
            "edition" to meta.edition.toString(),
            "maxEdition" to meta.maxEdition.toString(),
        )
    } catch (_: Exception) {
        mapOf("metaURI" to cadenceParser.string(logEvent.event.fields["metadata"]!!))
    }

    override fun creator(logEvent: FlowLogEvent) = try {
        Flow.unmarshall(VersusArtMetadata::class, logEvent.event.fields["metadata"]!!).artistAddress
    } catch (_: Exception) {
        logEvent.event.eventId.contractAddress.formatted
    }
}
