package com.rarible.flow.scanner.model

enum class RaribleNftEventType(val eventName: String) {
    WITHDRAW("Withdraw"),
    DEPOSIT("Deposit"),
    MINT("Mint"),
    BURN("Destroy"),
    ;

    companion object {
        private val NAME_MAP = RaribleNftEventType.values().associateBy { it.eventName }

        val EVENT_NAMES = NAME_MAP.keys

        fun fromEventName(eventName: String): RaribleNftEventType? {
            return NAME_MAP[eventName]
        }
    }
}