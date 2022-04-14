package com.rarible.flow.scanner.listener.activity

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.listener.NFTActivityMaker
import org.springframework.stereotype.Component

@Component
class IrNFTActivityMaker : NFTActivityMaker() {
    override val contractName = Contracts.IR_NFT.contractName

    override fun tokenId(logEvent: FlowLogEvent) =
        cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent) = mapOf(
        "collectionID" to "${cadenceParser.int(logEvent.event.fields["collectionID"]!!)}",
        "itemID" to "${cadenceParser.int(logEvent.event.fields["itemID"]!!)}",
        "serial" to "${cadenceParser.int(logEvent.event.fields["serial"]!!)}",
    )
}
