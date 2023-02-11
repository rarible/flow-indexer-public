package com.rarible.flow.scanner.eventprocessor

import com.rarible.flow.scanner.model.IndexerEvent

/**
 * Indexer event's processor
 */
interface IndexerEventsProcessor {

    fun isSupported(event: IndexerEvent): Boolean

    suspend fun process(event: IndexerEvent)
}
