package com.rarible.flow.core.domain

import com.rarible.blockchain.scanner.consumer.LogRecordMapper
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.util.addIn
import com.rarible.core.common.EventTimeMarks

data class GeneralFlowLogRecordEvent(
    val record: FlowLogEvent,
    val reverted: Boolean,
    val eventTimeMarks: EventTimeMarks,
) {
    companion object {
        fun logRecordMapper(): LogRecordMapper<GeneralFlowLogRecordEvent> {
            return object : LogRecordMapper<GeneralFlowLogRecordEvent> {
                override fun map(event: GeneralFlowLogRecordEvent): LogRecordEvent {
                    return LogRecordEvent(event.record, event.reverted, event.eventTimeMarks.addIn())
                }
            }
        }
    }
}