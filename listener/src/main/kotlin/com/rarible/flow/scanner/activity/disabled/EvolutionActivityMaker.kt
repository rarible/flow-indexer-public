package com.rarible.flow.scanner.activity.disabled

import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties

class EvolutionActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : NFTActivityMaker(flowLogRepository, txManager, properties) {

    override val contractName: String = Contracts.EVOLUTION.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = mint(logEvent).tokenId

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = mapOf(
        "itemId" to "${cadenceParser.int(logEvent.event.fields["itemId"]!!)}",
        "setId" to "${cadenceParser.int(logEvent.event.fields["setId"]!!)}",
        "serialNumber" to "${cadenceParser.int(logEvent.event.fields["serialNumber"]!!)}"
    )

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.EVOLUTION.staticRoyalties(chainId)
    }
}
