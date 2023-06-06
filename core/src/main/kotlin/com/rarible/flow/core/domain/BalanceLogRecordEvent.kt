package com.rarible.flow.core.domain

import com.rarible.blockchain.scanner.consumer.LogRecordMapper
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.util.addIn
import com.rarible.core.common.EventTimeMarks

data class BalanceLogRecordEvent(
    val record: BalanceHistory,
    val reverted: Boolean,
    val eventTimeMarks: EventTimeMarks,
) {
    companion object {
        fun logRecordMapper(): LogRecordMapper<BalanceLogRecordEvent> {
            return object : LogRecordMapper<BalanceLogRecordEvent> {
                override fun map(event: BalanceLogRecordEvent): LogRecordEvent {
                    return LogRecordEvent(event.record, event.reverted, event.eventTimeMarks.addIn())
                }
            }
        }
    }
}

