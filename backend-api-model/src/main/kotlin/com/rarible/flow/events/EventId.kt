package com.rarible.flow.events

import org.onflow.sdk.Address
import org.onflow.sdk.toHex


data class EventId(
    val type: String,
    val contractAddress: Address,
    val contractName: String,
    val eventName: String
) {
    override fun toString(): String {
        return "$type.${contractAddress.bytes.toHex()}.$contractName.$eventName"
    }

    companion object {
        fun of(str: String): EventId {
            val parts = str.split('.')
            if(parts.size == 4) {
                return EventId(
                    parts[0],
                    Address(parts[1]),
                    parts[2],
                    parts[3]
                )
            } else {
                throw IllegalArgumentException("Failed to parse EventId from [$str]")
            }
        }
    }
}
