package com.rarible.flow.scanner.listener

import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSaveAll
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.IndexerEventService
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class VersusArtEventListener(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val itemRepository: ItemRepository,
    private val indexerEventService: IndexerEventService,
    private val protocolEventPublisher: ProtocolEventPublisher,
) : FlowLogEventListener {

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        try {
            val result = processBlockEvents(blockEvent)
            if (result.isNotEmpty()) {
                val saved = itemHistoryRepository.coSaveAll(result)
                val ids = saved.map { it.activity }
                    .filterIsInstance<NFTActivity>()
                    .map { ItemId(it.contract, it.tokenId) }
                    .toSet()
                val items = itemRepository.findAllByIdIn(ids).asFlow().toSet()

                saved.sortedBy { it.date }.groupBy { it.log.transactionHash }.forEach { (_, histories) ->
                    histories.sortedBy { it.log.eventIndex }.forEach { history ->
                        indexerEventService.processEvent(
                            IndexerEvent(
                                history = history,
                                source = blockEvent.event.eventSource,
                                item = (history.activity as? NFTActivity)?.let { a ->
                                    items.find { it.contract == a.contract && it.tokenId == a.tokenId }
                                }
                            )
                        )
                        if (blockEvent.event.eventSource != Source.REINDEX) {
                            logger.info("Send activity [${history.id}] to kafka!")
                            protocolEventPublisher.activity(history).ensureSuccess()
                        }
                    }

                }
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Throwable(e)
        }
    }

    private suspend fun processBlockEvents(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>): List<ItemHistory> {
        val events = blockEvent.records.filterIsInstance<FlowLogEvent>()
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

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) = Unit

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
