package com.rarible.flow.scanner.model

enum class NFTStorefrontEventType(val eventName: String) {
    LISTING_AVAILABLE("ListingAvailable"),
    LISTING_COMPLETED("ListingCompleted"),
    ;

    companion object {
        private val NAME_MAP = NFTStorefrontEventType.values().associateBy { it.eventName }

        val EVENT_NAMES = NAME_MAP.keys

        fun fromEventName(eventName: String): NFTStorefrontEventType? {
            return NAME_MAP[eventName]
        }
    }
}