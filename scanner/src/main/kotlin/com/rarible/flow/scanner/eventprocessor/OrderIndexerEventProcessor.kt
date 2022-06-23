package com.rarible.flow.scanner.eventprocessor

import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowNftOrderActivityBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.OrderService
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "indexer")
class OrderIndexerEventProcessor(
    private val orderService: OrderService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderConverter: OrderToDtoConverter
) : IndexerEventsProcessor {

    private val supportedTypes = arrayOf(
        FlowActivityType.LIST,
        FlowActivityType.SELL,
        FlowActivityType.CANCEL_LIST,
        FlowActivityType.BID,
        FlowActivityType.CANCEL_BID,
    )

    override fun isSupported(event: IndexerEvent): Boolean = event.activityType() in supportedTypes

    override suspend fun process(event: IndexerEvent) {
        when (event.activityType()) {
            FlowActivityType.LIST -> list(event)
            FlowActivityType.SELL -> close(event)
            FlowActivityType.CANCEL_LIST -> orderCancelled(event)
            FlowActivityType.BID -> bid(event)
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
            orderService.enrichCancelList(activity.hash)
            val o = orderService.openList(activity, event.item)
            sendUpdate(event, o)
        }
    }

    private suspend fun close(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivitySell
        return when (activity.left.asset) {
            is FlowAssetNFT -> orderClose(event)
            is FlowAssetFungible -> acceptBid(event)
            else -> throw IllegalStateException("Invalid order asset: ${activity.left.asset}, in activity: $activity")
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
            sendUpdate(event, o)
            orderService.enrichTransfer(event.history.log.transactionHash, activity.left.maker, activity.right.maker)
                ?.also { sendHistoryUpdate(event, it) }
        }
    }

    private suspend fun orderCancelled(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivityCancelList
        withSpan("cancelOrderEvent", type = "event", labels = listOf("hash" to activity.hash)) {
            orderService.enrichCancelList(activity.hash)
            val o = orderService.cancel(activity, event.item)
            sendUpdate(event, o)
        }
    }

    private suspend fun bid(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivityBid
        withSpan("openBidEvent", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            orderService.enrichCancelBid(activity.hash)
            val o = orderService.openBid(activity, event.item)
            sendUpdate(event, o)
        }
    }

    private suspend fun acceptBid(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivitySell
        withSpan("acceptBidEvent", type = "event", labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")) {
            val o = orderService.closeBid(activity, event.item)
            sendUpdate(event, o)
            orderService.enrichTransfer(event.history.log.transactionHash, activity.right.maker, activity.left.maker)
                ?.also { sendHistoryUpdate(event, it) }
        }
    }

    private suspend fun cancelBid(event: IndexerEvent) {
        val activity = event.history.activity as FlowNftOrderActivityCancelBid
        withSpan("cancelBidEvent", type = "event", labels = listOf("hash" to activity.hash)) {
            orderService.enrichCancelBid(activity.hash)
            val o = orderService.cancelBid(activity, event.item)
            sendUpdate(event, o)
        }
    }

    private suspend fun sendHistoryUpdate(
        event: IndexerEvent,
        itemHistory: ItemHistory,
    ) {
        withSpan("sendOrderUpdate", "network") {
            if (event.source != Source.REINDEX) {
                protocolEventPublisher.activity(itemHistory)
            }
        }
    }

    private suspend fun sendUpdate(
        event: IndexerEvent,
        o: Order
    ) {
        withSpan("sendOrderUpdate", "network") {
            if (event.source != Source.REINDEX) {
                protocolEventPublisher.onOrderUpdate(o, orderConverter)
            }
        }
    }

}
