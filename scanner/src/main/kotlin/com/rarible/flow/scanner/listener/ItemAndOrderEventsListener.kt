package com.rarible.flow.scanner.listener

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.NFTActivity
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSaveAll
import com.rarible.flow.core.util.Log
import com.rarible.flow.scanner.activitymaker.ActivityMaker
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.IndexerEventService
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class ItemAndOrderEventsListener(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val nftActivityMakers: List<ActivityMaker>,
    private val indexerEventService: IndexerEventService,
    private val itemRepository: ItemRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
) : FlowLogEventListener {

    private val logger by Log()

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        val history: MutableList<ItemHistory> = mutableListOf()
        try {
            blockEvent.records.filterIsInstance<FlowLogEvent>()
                .groupBy { Pair(it.log.transactionHash, it.event.eventId.collection()) }
                .forEach { entry ->
                    nftActivityMakers.find { it.isSupportedCollection(entry.key.second) }?.let { maker ->
                        val activities = maker.activities(entry.value).map { entry ->
                            ItemHistory(
                                log = entry.key,
                                activity = entry.value,
                                date = entry.value.timestamp
                            )
                        }

                        logger.info("{} produced {} activities", maker::class, activities.size)
                        history.addAll(activities)
                    }
                }

            if (history.isNotEmpty()) {
                val saved = itemHistoryRepository.coSaveAll(history)
                val ids = saved.filter { it.activity is NFTActivity }.map {
                    val a = it.activity as NFTActivity
                    ItemId(a.contract, a.tokenId)
                }.toSet()
                val items = itemRepository.findAllByIdIn(ids).asFlow().toSet()

                saved.sortedBy { it.date }.groupBy { it.log.transactionHash }.forEach { tx ->
                    tx.value.sortedBy { it.log.eventIndex }.forEach { h ->
                        if (blockEvent.event.eventSource != Source.REINDEX) {
                            logger.info("Send activity [${h.id}] to kafka!")
                            protocolEventPublisher.activity(h).ensureSuccess()
                        }
                        indexerEventService.processEvent(
                            IndexerEvent(
                                history = h,
                                source = blockEvent.event.eventSource,
                                item = if (h.activity is NFTActivity) {
                                    val a = h.activity as NFTActivity
                                    items.find { it.contract == a.contract && it.tokenId == a.tokenId }
                                } else null)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Throwable(e)
        }
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing */
    }
}
