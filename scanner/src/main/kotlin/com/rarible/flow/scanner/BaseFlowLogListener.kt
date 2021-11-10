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
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Component
class BaseFlowLogListener(
    private val indexerEventService: IndexerEventService,
    private val protocolEventPublisher: ProtocolEventPublisher
) : FlowLogEventListener {

    private val events: ConcurrentHashMap<ItemId, TreeSet<IndexerEvent>> = ConcurrentHashMap()

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        when(blockEvent.event.eventSource) {
            Source.REINDEX, Source.PENDING -> {
                blockEvent.records.filterIsInstance<ItemHistory>().asFlow().collect {
                    events.getOrPut(ItemId(contract = it.activity.contract, tokenId = it.activity.tokenId)) {
                        TreeSet(compareBy<IndexerEvent> { it.history.log.transactionHash }.thenBy { it.history.log.eventIndex })
                    }.add(IndexerEvent(history = it, source = blockEvent.event.eventSource))

                }
            }
            Source.BLOCKCHAIN -> {
                blockEvent.records.filterIsInstance<ItemHistory>().asFlow().collect {
                    indexerEventService.processEvent(IndexerEvent(history = it, source = Source.BLOCKCHAIN))
                    protocolEventPublisher.activity(it)
                }
            }
        }


    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing */
        log.warn("onPendingLogsDropped not realized yet!")
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    fun process() = runBlocking {
        events.filter { it.value.size >= 10 }.forEach {
            val m = events.replace(it.key, TreeSet<IndexerEvent>(compareBy<IndexerEvent> { it.history.log.transactionHash }.thenBy { it.history.log.eventIndex }))!!
            m.asFlow().onEach {
                indexerEventService.processEvent(it)
                if(it.source != Source.REINDEX) {
                    protocolEventPublisher.activity(it.history)
                }
            }.collect()
        }
    }

    companion object {
        private val log by Log()
    }
}
