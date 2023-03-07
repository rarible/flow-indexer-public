package com.rarible.flow.scanner.model

import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.TokenId

sealed class ChainMonstersNftEvents(event: FlowLogEvent) : GeneralNftEvent(event) {
    override val tokenId: TokenId = CadenceParser.parser.long(event.event.fields["NFTID"] ?: event.event.fields["id"]!!)
}

class ChainMonstersMintEvent(event: FlowLogEvent) : ChainMonstersNftEvents(event), MintEvent

class ChainMonstersBurnEvent(event: FlowLogEvent) : ChainMonstersNftEvents(event), BurnEvent

class ChainMonstersDepositEvent(event: FlowLogEvent) : ChainMonstersNftEvents(event), DepositEvent {
    private val delegate = GeneralDepositEvent(event)

    override val to: String
        get() = delegate.to

    override val optionalTo: String?
        get() = delegate.optionalTo
}

class ChainMonstersWithdrawEvent(event: FlowLogEvent) : GeneralNftEvent(event), WithdrawEvent {
    private val delegate = GeneralWithdrawEvent(event)
    override val from: String
        get() = delegate.from

    override val optionalFrom: String?
        get() = delegate.optionalFrom
}
