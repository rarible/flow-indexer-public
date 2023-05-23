package com.rarible.flow.scanner.model

enum class AdminMintCardEventType(override val eventName: String) : AbstractEventType {
    ADMIN_MINT_CARD("AdminMintCard"),
    ;

    companion object {
        private val NAME_MAP = values().associateBy { it.eventName }

        val EVENT_NAMES = NAME_MAP.keys

        fun fromEventName(eventName: String): AdminMintCardEventType? {
            return NAME_MAP[eventName]
        }
    }
}