package com.rarible.flow.scanner.model

import com.nftco.flow.sdk.FlowEvent
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.event.EventMessageCadenceConverter

sealed class GeneralNFTEvent(protected val event: EventMessage) : NFTEvent {
    override val id: TokenId = CadenceParser.parser.long(event.fields["id"]!!)
}

class GeneralMintEvent(event: EventMessage) : GeneralNFTEvent(event), MintEvent

class GeneralBurnEvent(event: EventMessage) : GeneralNFTEvent(event), BurnEvent

class GeneralDepositEvent(event: EventMessage) : GeneralNFTEvent(event), DepositEvent {
    override val to: String
        get() = optionalTo ?: error("Unexpected null filed 'to' for event $event")

    override val optionalTo = CadenceParser.parser.optional(event.fields["to"]!!) { address(it) }
}

class GeneralWithdrawEvent(event: EventMessage) : GeneralNFTEvent(event), WithdrawEvent {
    override val from: String
        get() = optionalFrom ?: error("Unexpected null filed 'from' for event $event")

    override val optionalFrom = CadenceParser.parser.optional(event.fields["from"]!!) { address(it) }
}

