package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.IndexerEventService
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BaseFlowLogListener(
    private val indexerEventService: IndexerEventService,
    private val protocolEventPublisher: ProtocolEventPublisher
) : FlowLogEventListener {

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        blockEvent.records.filterIsInstance<ItemHistory>().asFlow().collect {
            indexerEventService.processEvent(
                IndexerEvent(
                    activity = it.activity,
                    source = blockEvent.event.eventSource
                )
            )

            if(blockEvent.event.eventSource != Source.REINDEX) {
                protocolEventPublisher.activity(it.activity, it)
            }
        }
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing */
        log.warn("onPendingLogsDropped not realized yet!")
    }

    companion object {
        val log by Log()
    }
}
