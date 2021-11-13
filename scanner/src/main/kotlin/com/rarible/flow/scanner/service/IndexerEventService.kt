package com.rarible.flow.scanner.service

import com.rarible.flow.scanner.eventlisteners.IndexerEventsProcessor
import com.rarible.flow.scanner.model.IndexerEvent
import org.springframework.stereotype.Service

@Service
class IndexerEventService(
    private val processors: List<IndexerEventsProcessor>
) {
    suspend fun processEvent(event: IndexerEvent) {
        val p = processors.firstOrNull { it.isSupported(event) }
            ?: throw IllegalStateException("Not found processor for indexer event [${event.activityType()}]")
        p.process(event)
    }
}
