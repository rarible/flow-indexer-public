package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.WithdrawnActivity
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.IndexerEventService
import org.springframework.stereotype.Component

@Component
class BaseFlowLogListener(
    private val indexerEventService: IndexerEventService,
    private val itemRepository: ItemRepository,
) : FlowLogEventListener {

    private val log by Log()

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        blockEvent.records.filterIsInstance<ItemHistory>()
            .groupBy {
                ItemId(contract = it.activity.contract, tokenId = it.activity.tokenId)
            }.forEach { entry ->
                val itemEvents = entry.value.filter {
                    it.activity.type in arrayOf(
                        FlowActivityType.MINT,
                        FlowActivityType.WITHDRAWN,
                        FlowActivityType.DEPOSIT,
                        FlowActivityType.BURN
                    )
                }.filterNot { //rem
                    if (it.activity is WithdrawnActivity) (it.activity as WithdrawnActivity).from.isNullOrEmpty() else false
                }.sortedBy { it.log.eventIndex }.chunked(2) //NFT events shouldn't be single

                val orderEvents =
                    entry.value.filter {
                        it.activity.type in arrayOf(
                            FlowActivityType.LIST,
                            FlowActivityType.CANCEL_LIST,
                            FlowActivityType.SELL
                        )
                    }.sortedBy { it.log.eventIndex }.chunked(1)

                val item = itemRepository.coFindById(entry.key)
                (itemEvents + orderEvents).map {
                    IndexerEvent(
                        history = it,
                        source = blockEvent.event.eventSource,
                        item = item
                    )
                }.forEach { indexerEventService.processEvent(it) }
            }
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing */
        log.warn("onPendingLogsDropped not realized yet!")
    }
}
