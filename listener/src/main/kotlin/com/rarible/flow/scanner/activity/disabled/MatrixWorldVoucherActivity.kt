package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker

class MatrixWorldVoucherActivity(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
): NFTActivityMaker(flowLogRepository,txManager, properties) {

    override val contractName: String = Contracts.MATRIX_WORLD_VOUCHER.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val name by logEvent.event.fields
        val description by logEvent.event.fields
        val animationUrl by logEvent.event.fields
        val hash by logEvent.event.fields
        val type by logEvent.event.fields
        val cp = JsonCadenceParser()
        return mapOf(
            "name" to cp.string(name),
            "description" to cp.string(description),
            "animationUrl" to cp.string(animationUrl),
            "hash" to cp.string(hash),
            "type" to cp.string(type)
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.MATRIX_WORLD_VOUCHER.staticRoyalties(chainId)
    }
}