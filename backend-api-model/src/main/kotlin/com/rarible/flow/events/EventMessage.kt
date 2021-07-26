package com.rarible.flow.events

import java.time.LocalDateTime

typealias TokenId = Long

data class EventMessage(
    val eventId: EventId,
    val fields: Map<String, Any?>,
    var timestamp: LocalDateTime,
    var blockInfo: BlockInfo
) {
    companion object {
        fun getTopic(environment: String) =
            "protocol.$environment.flow.scanner.nft.item"
    }
}

