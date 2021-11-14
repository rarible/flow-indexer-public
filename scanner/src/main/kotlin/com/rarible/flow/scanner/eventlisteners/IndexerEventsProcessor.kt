package com.rarible.flow.scanner.eventlisteners

import com.rarible.flow.scanner.model.IndexerEvent

/**
 * Indexer event's processor
 */
interface IndexerEventsProcessor {

    fun isSupported(event: IndexerEvent): Boolean

    suspend fun process(event: IndexerEvent)
}
