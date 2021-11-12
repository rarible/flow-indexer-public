package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.IndexerEventService
import org.springframework.stereotype.Component

@Component
class BaseFlowLogListener(
    private val indexerEventService: IndexerEventService,
    private val protocolEventPublisher: ProtocolEventPublisher
) : FlowLogEventListener {

    private val log by Log()

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        blockEvent.records.filterIsInstance<ItemHistory>().groupBy { ItemId(contract = it.activity.contract, tokenId = it.activity.tokenId) }.forEach { entry ->
            entry.value.forEach {
                indexerEventService.processEvent(IndexerEvent(history = it, source = blockEvent.event.eventSource))

                if (blockEvent.event.eventSource != Source.REINDEX) {
                    protocolEventPublisher.activity(it)
                }
            }
        }
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing */
        log.warn("onPendingLogsDropped not realized yet!")
    }
}
