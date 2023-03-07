package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.cadence.StringField
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker

class MatrixWorldFlowFestNFTActivity(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
): NFTActivityMaker(flowLogRepository, txManager, properties) {

    override val contractName: String = Contracts.MATRIX_WORLD_FLOW_FEST.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = mint(logEvent).tokenId

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val name: StringField by logEvent.event.fields
        val description: StringField by logEvent.event.fields
        val animationUrl: StringField by logEvent.event.fields
        val hash: StringField by logEvent.event.fields
        val type: StringField by logEvent.event.fields

        return mapOf(
            "name" to name.value!!,
            "description" to description.value!!,
            "animationUrl" to animationUrl.value!!,
            "hash" to hash.value!!,
            "type" to type.value!!
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.MATRIX_WORLD_FLOW_FEST.staticRoyalties(chainId)
    }
}
