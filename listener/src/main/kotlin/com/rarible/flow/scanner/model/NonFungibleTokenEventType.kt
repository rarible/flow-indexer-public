package com.rarible.flow.scanner.model

enum class NonFungibleTokenEventType(val eventName: String) {
    WITHDRAW("Withdraw"),
    DEPOSIT("Deposit"),
    MINT("Mint"),
    BURN("Burn"),
    ;

    fun full(contract: String): String {
        return "$contract.${eventName}"
    }

    companion object {
        private val NAME_MAP = NonFungibleTokenEventType.values().associateBy { it.eventName }

        val EVENT_NAMES = NAME_MAP.keys

        fun fromEventName(eventName: String): NonFungibleTokenEventType? {
            return NAME_MAP[eventName]
        }
    }
}