package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties

class JambbMomentsActivity(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
): NFTActivityMaker(flowLogRepository, txManager, properties) {

    override val contractName: String = Contracts.JAMBB_MOMENTS.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(
        logEvent.event.fields["momentID"] ?: logEvent.event.fields["id"]!!
    )

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val momentID: UInt64NumberField by logEvent.event.fields
        val contentID: UInt64NumberField by logEvent.event.fields
        val contentEditionID: UInt64NumberField by logEvent.event.fields
        val serialNumber: UInt64NumberField by logEvent.event.fields
        val seriesID: UInt64NumberField by logEvent.event.fields
        val setID: UInt64NumberField by logEvent.event.fields

        return mapOf(
            "momentID" to momentID.value!!,
            "contentID" to contentID.value!!,
            "contentEditionID" to contentEditionID.value!!,
            "serialNumber" to serialNumber.value!!,
            "seriesID" to seriesID.value!!,
            "setID" to setID.value!!,
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.JAMBB_MOMENTS.staticRoyalties(chainId)
    }
}
