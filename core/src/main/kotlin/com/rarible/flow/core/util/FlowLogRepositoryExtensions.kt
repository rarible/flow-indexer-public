@file:Suppress("UNCHECKED_CAST")

package com.rarible.flow.core.util

import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.core.domain.FlowLogEvent
import kotlinx.coroutines.flow.Flow

suspend fun FlowLogRepository.findAfterEventIndex(
    transactionHash: String,
    afterEventIndex: Int,
): Flow<FlowLogEvent> {
    return findAfterEventIndex(
        transactionHash = transactionHash,
        afterEventIndex = afterEventIndex,
        entityType = FlowLogEvent::class.java,
        collection = FlowLogEvent.COLLECTION
    ) as Flow<FlowLogEvent>
}

suspend fun FlowLogRepository.findBeforeEventIndex(
    transactionHash: String,
    beforeEventIndex: Int,
): Flow<FlowLogEvent> {
    return this.findBeforeEventIndex(
        transactionHash = transactionHash,
        beforeEventIndex = beforeEventIndex,
        entityType = FlowLogEvent::class.java,
        collection = FlowLogEvent.COLLECTION
    ) as Flow<FlowLogEvent>
}
