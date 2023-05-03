package com.rarible.flow.api.service.meta

data class MetaEventType(
    val eventType: String,
    val id: String = DEFAULT_ID,
) {

    private companion object {

        const val DEFAULT_ID = "id"
    }
}