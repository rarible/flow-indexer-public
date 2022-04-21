package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.listener.NFTActivityMaker
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BarterYardPackActivityMaker(
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId
): NFTActivityMaker() {
    override val contractName: String = Contracts.BARTER_YARD_PACK.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = mapOf(
        "packPartId" to "${cadenceParser.int(logEvent.event.fields["packPartId"]!!)}",
        "edition" to "${cadenceParser.uint(logEvent.event.fields["edition"]!!)}"
    )

    override fun royalties(logEvent: FlowLogEvent): List<Part> = Contracts.BARTER_YARD_PACK.staticRoyalties(chainId)
}
