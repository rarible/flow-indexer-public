package com.rarible.flow.scanner.model

import com.rarible.flow.core.domain.TokenId

interface NFTEvent {
    val tokenId: TokenId
    val collection: String
    val contractAddress: String
    val transactionHash: String
    val eventIndex: Int

    fun sameNftEvent(other: NFTEvent): Boolean {
        return this.tokenId == other.tokenId && this.collection == other.collection
    }
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

