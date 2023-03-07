package com.rarible.flow.scanner.activity.disabled

import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties

class BarterYardPackActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : NFTActivityMaker(flowLogRepository, txManager, properties) {

    override val contractName: String = Contracts.BARTER_YARD_PACK.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = mapOf(
        "packPartId" to "${cadenceParser.int(logEvent.event.fields["packPartId"]!!)}",
        "edition" to "${cadenceParser.uint(logEvent.event.fields["edition"]!!)}"
    )

    override fun royalties(logEvent: FlowLogEvent): List<Part> = Contracts.BARTER_YARD_PACK.staticRoyalties(chainId)
}
