package com.rarible.flow.scanner.model

import com.rarible.flow.core.domain.TokenId

interface NFTEvent {
    val id: TokenId
}

interface MintEvent : NFTEvent

interface BurnEvent : NFTEvent

interface DepositEvent : NFTEvent {
    val to: String
    val optionalTo: String?
}

interface WithdrawEvent : NFTEvent {
    val from: String
    val optionalFrom: String?
}

