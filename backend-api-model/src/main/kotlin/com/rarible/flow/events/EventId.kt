package com.rarible.flow.events

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.bytesToHex


data class EventId(
    val type: String,
    val contractAddress: FlowAddress,
    val contractName: String,
    val eventName: String
) {
    override fun toString(): String {
        return "$type.${contractAddress.bytes.bytesToHex()}.$contractName.$eventName"
    }

    fun contractEvent(): String = "$contractName.$eventName"

    fun nft(collectionName: String = "NFT"): String {
        return "$type.${contractAddress.bytes.bytesToHex()}.$contractName.$collectionName"
    }

    companion object {
        fun of(str: String): EventId {
            val parts = str.split('.')
            if(parts.size == 4) {
                return EventId(
                    parts[0],
                    FlowAddress(parts[1]),
                    parts[2],
                    parts[3]
                )
            } else {
                throw IllegalArgumentException("Failed to parse EventId from [$str]")
            }
        }
    }
}
