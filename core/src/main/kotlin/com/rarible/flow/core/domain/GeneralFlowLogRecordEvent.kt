package com.rarible.flow.core.domain

import com.rarible.blockchain.scanner.consumer.LogRecordMapper
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent

data class GeneralFlowLogRecordEvent(
    val record: FlowLogEvent,
    val reverted: Boolean
) {
    companion object {
        fun logRecordMapper(): LogRecordMapper<GeneralFlowLogRecordEvent> {
            return object : LogRecordMapper<GeneralFlowLogRecordEvent> {
                override fun map(event: GeneralFlowLogRecordEvent): LogRecordEvent {
                    return LogRecordEvent(event.record, event.reverted)
                }
            }
        }
    }
}