package com.rarible.flow.scanner.model

import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.TokenId

sealed class GeneralNFTEvent(protected val event: FlowLogEvent) : NFTEvent {
    override val tokenId: TokenId = CadenceParser.parser.long(event.event.fields["id"]!!)
    override val collection: String = event.event.eventId.collection()
    override val contractAddress: String = event.event.eventId.contractAddress.formatted
    override val transactionHash: String = event.log.transactionHash
    override val eventIndex: Int = event.log.eventIndex
}

class GeneralMintEvent(event: FlowLogEvent) : GeneralNFTEvent(event), MintEvent

class GeneralBurnEvent(event: FlowLogEvent) : GeneralNFTEvent(event), BurnEvent

class GeneralDepositEvent(event: FlowLogEvent) : GeneralNFTEvent(event), DepositEvent {
    override val to: String
        get() = optionalTo ?: error("Unexpected null filed 'to' for event $event")

    override val optionalTo = CadenceParser.parser.optional(event.event.fields["to"]!!) { address(it) }
}

class GeneralWithdrawEvent(event: FlowLogEvent) : GeneralNFTEvent(event), WithdrawEvent {
    override val from: String
        get() = optionalFrom ?: error("Unexpected null filed 'from' for event $event")

    override val optionalFrom = CadenceParser.parser.optional(event.event.fields["from"]!!) { address(it) }
}

