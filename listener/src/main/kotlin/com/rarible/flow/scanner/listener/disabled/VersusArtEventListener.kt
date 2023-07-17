package com.rarible.flow.scanner.listener.disabled

import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withSpan
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.NFTActivity
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSaveAll
import com.rarible.flow.core.util.Log
import com.rarible.flow.core.util.offchainEventMarks
import com.rarible.flow.scanner.listener.GeneralFlowLogListener
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.model.Listeners
import com.rarible.flow.scanner.model.SubscriberGroups
import com.rarible.flow.scanner.service.IndexerEventService
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow

class VersusArtEventListener(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val itemRepository: ItemRepository,
    private val indexerEventService: IndexerEventService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    environmentInfo: ApplicationEnvironmentInfo
) : GeneralFlowLogListener(
    name = Listeners.VERSUS_ART,
    flowGroupId = SubscriberGroups.VERSUS_ART_HISTORY,
    environmentInfo = environmentInfo
) {
    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        try {
            val result = processBlockEvents(events)
            if (result.isNotEmpty()) {
                val saved = itemHistoryRepository.coSaveAll(result)
                val ids = saved.map { it.activity }
                    .filterIsInstance<NFTActivity>()
                    .map { ItemId(it.contract, it.tokenId) }
                    .toSet()
                val items = itemRepository.findAllByIdIn(ids).asFlow().toSet()

                saved.sortedBy { it.date }.groupBy { it.log.transactionHash }.forEach { (_, histories) ->
                    histories.sortedBy { it.log.eventIndex }.forEach { history ->
                        val eventTimeMarks = offchainEventMarks()
                        indexerEventService.processEvent(
                            IndexerEvent(
                                history = history,
                                item = (history.activity as? NFTActivity)?.let { a ->
                                    items.find { it.contract == a.contract && it.tokenId == a.tokenId }
                                },
                                eventTimeMarks = eventTimeMarks // TODO send it in right way if activated
                            )
                        )
                        logger.info("Send activity [${history.id}] to kafka!")
                        // TODO send it in right way if activated
                        protocolEventPublisher.activity(history, false, eventTimeMarks)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Throwable(e)
        }
    }

    private suspend fun processBlockEvents(blockEvent: List<LogRecordEvent>): List<ItemHistory> {
        val events = blockEvent.filterIsInstance<FlowLogEvent>()
        val custom = events.filter { it.type == FlowLogType.CUSTOM }
        if (custom.isEmpty()) return emptyList()

        return withSpan("generateNftActivities", "event") {
            when (custom.first().customType) {
                "DropDestroyed" -> events
                    .filter { it.type == FlowLogType.DEPOSIT }
                    .map { it.burnItemHistory() }
                "Settle" -> events
                    .filter { it.type == FlowLogType.DEPOSIT && it.to == it.contractAddress }
                    .map { it.burnItemHistory() }
                else -> emptyList()
            }
        }
    }

    private val cadenceParser: JsonCadenceParser = JsonCadenceParser()

    private val logger by Log()

    private fun FlowLogEvent.burnItemHistory(): ItemHistory {
        val log = log.let { it.copy(eventIndex = Int.MAX_VALUE - it.eventIndex) }
        return ItemHistory(log = log, activity = burnActivity(), date = log.timestamp)
    }

    private fun FlowLogEvent.burnActivity() =
        BurnActivity(contract = collection, tokenId = tokenId, owner = to, timestamp = log.timestamp)

    private val FlowLogEvent.customType
        get() = event.eventId.eventName

    private val FlowLogEvent.contractAddress
        get() = event.eventId.contractAddress.formatted

    private val FlowLogEvent.collection: String
        get() = event.eventId.collection()

    private val FlowLogEvent.tokenId: Long
        get() = cadenceParser.long(event.fields["id"]!!)

    private val FlowLogEvent.to: String?
        get() = cadenceParser.optional(event.fields["to"]!!, JsonCadenceParser::address)
}
