package com.rarible.flow.scanner.listener

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.EventTimeMarks
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.GeneralFlowLogRecordEvent
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.NFTActivity
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSaveAll
import com.rarible.flow.core.util.offchainEventMarks
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.model.Listeners
import com.rarible.flow.scanner.model.SubscriberGroups
import com.rarible.flow.scanner.service.IndexerEventService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemAndOrderEventsListener(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val nftActivityMakers: List<com.rarible.flow.scanner.activity.ActivityMaker>,
    private val indexerEventService: IndexerEventService,
    private val itemRepository: ItemRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    environmentInfo: ApplicationEnvironmentInfo,
) : GeneralFlowLogListener(
    name = Listeners.NFT_ORDER,
    flowGroupId = SubscriberGroups.NFT_ORDER_HISTORY,
    environmentInfo = environmentInfo
) {
    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {

        val history: MutableList<ItemHistoryEvent> = mutableListOf()
        val flowLogRecordsEvents = filterFlowLogRecords(events)
        val byLog = flowLogRecordsEvents.associateBy { it.record.log }

        flowLogRecordsEvents.groupBy { getKey(it) }
            .forEach { entry ->
                nftActivityMakers.find { it.isSupportedCollection(entry.key.collection) }?.let { maker ->
                    val flowEvents = entry.value.map { it.record }
                    val activities = maker.activities(flowEvents).map { entry ->
                        ItemHistoryEvent(
                            history = ItemHistory(
                                log = entry.key,
                                activity = entry.value,
                                date = entry.value.timestamp
                            ),
                            eventTimeMarks = byLog[entry.key]?.eventTimeMarks ?: offchainEventMarks()
                        )
                    }
                    logger.info("{} produced {} activities", maker::class, activities.size)
                    history.addAll(activities)
                }
            }

        if (history.isEmpty()) {
            return
        }

        itemHistoryRepository.coSaveAll(history.map { it.history })

        history.sortedBy { it.history.date }.groupBy { it.history.log.transactionHash }.forEach { tx ->
            tx.value.sortedBy { it.history.log.eventIndex }.forEach { itemHistoryEvent ->
                val h = itemHistoryEvent.history
                logger.info("Send activity [${h.id}] to kafka!")
                // Cancel list activity hasn't enough information yet to be published
                if (h.activity.isCancelList().not()) {
                    protocolEventPublisher.activity(h, false, itemHistoryEvent.eventTimeMarks)
                }
                val item = (h.activity as? NFTActivity)
                    ?.let { ItemId(it.contract, it.tokenId) }
                    ?.let { itemRepository.findById(it).awaitFirstOrNull() }

                indexerEventService.processEvent(
                    IndexerEvent(
                        history = h,
                        eventTimeMarks = itemHistoryEvent.eventTimeMarks,
                        item = item
                    )
                )
            }
        }
    }

    private fun filterFlowLogRecords(events: List<LogRecordEvent>): List<GeneralFlowLogRecordEvent> {
        return events.mapNotNull { event ->
            (event.record as? FlowLogEvent)?.let {
                GeneralFlowLogRecordEvent(
                    record = it,
                    reverted = event.reverted,
                    eventTimeMarks = event.eventTimeMarks
                )
            }
        }
    }

    private data class ItemHistoryEvent(
        val history: ItemHistory,
        val eventTimeMarks: EventTimeMarks,
    )

    private data class TransactionCollectionKey(
        val transactionHash: String,
        val collection: String,
    )

    private fun getKey(event: GeneralFlowLogRecordEvent): TransactionCollectionKey {
        return TransactionCollectionKey(
            transactionHash = event.record.log.transactionHash,
            collection = event.record.event.eventId.collection()
        )
    }

    private companion object {

        val logger: Logger = LoggerFactory.getLogger(ItemAndOrderEventsListener::class.java)
    }
}
