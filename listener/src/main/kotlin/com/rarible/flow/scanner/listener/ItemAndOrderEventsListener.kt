package com.rarible.flow.scanner.listener

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.NFTActivity
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSaveAll
import com.rarible.flow.scanner.activitymaker.ActivityMaker
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.model.Listeners
import com.rarible.flow.scanner.model.SubscriberGroups
import com.rarible.flow.scanner.service.IndexerEventService
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemAndOrderEventsListener(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val nftActivityMakers: List<ActivityMaker>,
    private val indexerEventService: IndexerEventService,
    private val itemRepository: ItemRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    environmentInfo: ApplicationEnvironmentInfo
) : GeneralFlowLogListener(
    name = Listeners.NFT_ORDER,
    flowGroupId = SubscriberGroups.NFT_ORDER_HISTORY,
    environmentInfo = environmentInfo
) {
    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        logger.info("[2296] onLogRecordEvents: $events")
        val history: MutableList<ItemHistory> = mutableListOf()
        try {
            events
                .map { event -> event.record }
                .filterIsInstance<FlowLogEvent>()
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
                        logger.info("Send activity [${h.id}] to kafka!")
                        protocolEventPublisher.activity(h).ensureSuccess()

                        logger.info("[2278] processEvent ${h.activity}")
                        indexerEventService.processEvent(
                            IndexerEvent(
                                history = h,
                                item = if (h.activity is NFTActivity) {
                                    val a = h.activity as NFTActivity
                                    items.find { it.contract == a.contract && it.tokenId == a.tokenId }
                                } else null)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("[2296] can't handle: $events", e)
            logger.error(e.message, e)
            throw Throwable(e)
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ItemAndOrderEventsListener::class.java)
    }

}
