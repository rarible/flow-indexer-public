package com.rarible.flow.events

data class EventMessage(
    val id: String,
    val fields: Map<String, String>
) {
    companion object {
        fun getTopic(environment: String) =
            "protocol.$environment.flow.indexer.nft.item"
    }
}