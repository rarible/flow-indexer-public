package com.rarible.flow.scanner.service

import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.activity.ActivityMaker
import org.springframework.stereotype.Component

@Component
class RecordKeyProvider(
    private val maker: List<ActivityMaker>,
) {
    fun getRecordKey(event: FlowLogEvent): String? {
        val iterator = maker.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.isSupportedCollection(event.event.eventId.collection()))
                return next.getItemId(event)?.toString() ?: continue
        }
        return null
    }
}
