package com.rarible.flow.scanner.model

import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.TokenId

sealed class GeneralNftEvent(protected val event: FlowLogEvent) : NFTEvent {
    override val tokenId: TokenId
        get() = CadenceParser.parser.long(event.event.fields["id"]!!)

    override val collection: String
        get() = event.event.eventId.collection()

    override val contractAddress: String
        get() = event.event.eventId.contractAddress.formatted

    override val transactionHash: String
        get() = event.log.transactionHash

    override val eventIndex: Int
        get() = event.log.eventIndex
}

class GeneralMintEvent(event: FlowLogEvent) : GeneralNftEvent(event), MintEvent

class GeneralBurnEvent(event: FlowLogEvent) : GeneralNftEvent(event), BurnEvent

class GeneralDepositEvent(event: FlowLogEvent) : GeneralNftEvent(event), DepositEvent {
    override val to: String
        get() = optionalTo ?: error("Unexpected null filed 'to' for event $event")

    override val optionalTo = CadenceParser.parser.optional(event.event.fields["to"]!!) { address(it) }
}

class GeneralWithdrawEvent(event: FlowLogEvent) : GeneralNftEvent(event), WithdrawEvent {
    override val from: String
        get() = optionalFrom ?: error("Unexpected null filed 'from' for event $event")

    override val optionalFrom = CadenceParser.parser.optional(event.event.fields["from"]!!) { address(it) }
}

