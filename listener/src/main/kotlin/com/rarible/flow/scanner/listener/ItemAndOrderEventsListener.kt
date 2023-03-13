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
    environmentInfo: ApplicationEnvironmentInfo
) : GeneralFlowLogListener(
    name = Listeners.NFT_ORDER,
    flowGroupId = SubscriberGroups.NFT_ORDER_HISTORY,
    environmentInfo = environmentInfo
) {
    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        val history: MutableList<ItemHistory> = mutableListOf()
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

            saved.sortedBy { it.date }.groupBy { it.log.transactionHash }.forEach { tx ->
                tx.value.sortedBy { it.log.eventIndex }.forEach { h ->
                    logger.info("Send activity [${h.id}] to kafka!")
                    protocolEventPublisher.activity(h)

                    val item = (h.activity as? NFTActivity)
                        ?.let { ItemId(it.contract, it.tokenId) }
                        ?.let { itemRepository.findById(it).awaitFirstOrNull() }

                    indexerEventService.processEvent(
                        IndexerEvent(
                            history = h,
                            item = item
                        )
                    )
                }
            }
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ItemAndOrderEventsListener::class.java)
    }

}
