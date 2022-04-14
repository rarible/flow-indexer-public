package com.rarible.flow.scanner.listener.activity

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.listener.NFTActivityMaker
import org.springframework.stereotype.Component

@Component
class IrVoucherActivityMaker : NFTActivityMaker() {
    override val contractName = Contracts.IR_VOUCHER.contractName

    override fun tokenId(logEvent: FlowLogEvent) =
        cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent) = mapOf(
        "dropID" to "${cadenceParser.int(logEvent.event.fields["dropID"]!!)}",
        "serial" to "${cadenceParser.int(logEvent.event.fields["serial"]!!)}",
    )
}
