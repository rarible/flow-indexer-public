package com.rarible.flow.events

import org.onflow.sdk.FlowAddress
import org.onflow.sdk.bytesToHex


data class EventId(
    val type: String,
    val contractAddress: FlowAddress,
    val contractName: String,
    val eventName: String
) {
    override fun toString(): String {
        return "$type.${contractAddress.bytes.bytesToHex()}.$contractName.$eventName"
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
