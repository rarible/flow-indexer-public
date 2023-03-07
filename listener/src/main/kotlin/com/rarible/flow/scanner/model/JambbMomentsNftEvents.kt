package com.rarible.flow.scanner.model

import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.TokenId

sealed class JambbMomentsNftEvents(event: FlowLogEvent) : GeneralNftEvent(event) {
    override val tokenId: TokenId = CadenceParser.parser.long(event.event.fields["momentID"] ?: event.event.fields["id"]!!)
}

class JambbMomentsMintEvent(event: FlowLogEvent) : JambbMomentsNftEvents(event), MintEvent

class JambbMomentsBurnEvent(event: FlowLogEvent) : JambbMomentsNftEvents(event), BurnEvent

class JambbMomentsDepositEvent(event: FlowLogEvent) : JambbMomentsNftEvents(event), DepositEvent {
    private val delegate = GeneralDepositEvent(event)

    override val to: String
        get() = delegate.to

    override val optionalTo: String?
        get() = delegate.optionalTo
}

class JambbMomentsWithdrawEvent(event: FlowLogEvent) : JambbMomentsNftEvents(event), WithdrawEvent {
    private val delegate = GeneralWithdrawEvent(event)
    override val from: String
        get() = delegate.from

    override val optionalFrom: String?
        get() = delegate.optionalFrom
}
