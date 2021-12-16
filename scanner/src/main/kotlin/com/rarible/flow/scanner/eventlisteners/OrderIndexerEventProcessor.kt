package com.rarible.flow.scanner.eventlisteners

import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.OrderService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class OrderIndexerEventProcessor(
    private val orderService: OrderService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderConverter: OrderToDtoConverter,
    private val itemRepository: ItemRepository
) : IndexerEventsProcessor {

    private val supportedTypes = arrayOf(
        FlowActivityType.LIST,
        FlowActivityType.SELL,
        FlowActivityType.CANCEL_LIST,
        FlowActivityType.BID,
        FlowActivityType.CANCEL_BID,
        FlowActivityType.ACCEPT_BID
    )

    override fun isSupported(event: IndexerEvent): Boolean = event.activityType() in supportedTypes

    override suspend fun process(event: IndexerEvent) {
        when (event.activityType()) {
            FlowActivityType.LIST -> list(event)
            FlowActivityType.SELL -> orderClose(event)
            FlowActivityType.CANCEL_LIST -> orderCancelled(event)
            FlowActivityType.BID -> bid(event)
            FlowActivityType.ACCEPT_BID -> acceptBid(event)
            FlowActivityType.CANCEL_BID -> cancelBid(event)
            else -> throw IllegalStateException("Unsupported order event! [${event.activityType()}]")
        }
    }

    private suspend fun list(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivityList
        withSpan(
            "listOrderEvent",
            type = "event",
            labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")
        ) {
            val o = orderService.openList(activity, event.item)
            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onOrderUpdate(o, orderConverter)
                itemRepository.findById(o.itemId).awaitSingleOrNull()?.let {
                    protocolEventPublisher.onItemUpdate(itemRepository.coSave(it.copy(listed = true)))
                }
            }
        }
    }

    private suspend fun orderClose(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivitySell
        withSpan(
            "closeOrderEvent",
            type = "event",
            labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")
        ) {
            val o = orderService.close(activity)

            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onOrderUpdate(o, orderConverter)
                itemRepository.findById(o.itemId).awaitSingleOrNull()?.let {
                    protocolEventPublisher.onItemUpdate(itemRepository.coSave(it.copy(listed = false)))
                }
            }
        }
    }

    private suspend fun orderCancelled(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivityCancelList
        withSpan("cancelOrderEvent", type = "event", labels = listOf("hash" to activity.hash)) {
            val o = orderService.cancel(activity, event.item)

            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onOrderUpdate(o, orderConverter)
                itemRepository.findById(o.itemId).awaitSingleOrNull()?.let {
                    protocolEventPublisher.onItemUpdate(itemRepository.coSave(it.copy(listed = false)))
                }
            }
        }
    }

    private suspend fun bid(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivityBid
        withSpan("openBidEvent", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val o = orderService.openBid(activity, event.item)
            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onOrderUpdate(o, orderConverter)
            }
        }
    }

    private suspend fun acceptBid(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivityBidAccept
        withSpan("acceptBidEvent", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val o = orderService.closeBid(activity, event.item)
            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onOrderUpdate(o, orderConverter)
            }
        }
    }

    private suspend fun cancelBid(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivityCancelBid
        withSpan("cancelBidEvent", type = "event", labels = listOf("hash" to activity.hash)) {
            val o = orderService.cancelBid(activity, event.item)
            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onOrderUpdate(o, orderConverter)
            }
        }
    }

}
