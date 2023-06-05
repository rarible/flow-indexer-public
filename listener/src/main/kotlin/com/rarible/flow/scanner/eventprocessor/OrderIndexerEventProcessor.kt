package com.rarible.flow.scanner.eventprocessor

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.withSpan
import com.rarible.core.common.EventTimeMarks
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
import com.rarible.flow.core.util.offchainEventMarks
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.OrderService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "indexer")
class OrderIndexerEventProcessor(
    private val orderService: OrderService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderConverter: OrderToDtoConverter
) : IndexerEventsProcessor {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val supportedTypes = arrayOf(
        FlowActivityType.LIST,
        FlowActivityType.SELL,
        FlowActivityType.CANCEL_LIST,
        FlowActivityType.BID,
        FlowActivityType.CANCEL_BID,
    )

    override fun isSupported(event: IndexerEvent): Boolean = event.activityType() in supportedTypes

    override suspend fun process(event: IndexerEvent) {
        val marks = event.eventTimeMarks ?: run {
            logger.warn("EventTimeMarks not found in OrderIndexerEvent")
            offchainEventMarks()
        }
        when (event.activityType()) {
            FlowActivityType.LIST -> list(event, marks)
            FlowActivityType.SELL -> close(event, marks)
            FlowActivityType.CANCEL_LIST -> orderCancelled(event, marks)
            FlowActivityType.BID -> bid(event, marks)
            FlowActivityType.CANCEL_BID -> cancelBid(event, marks)
            else -> throw IllegalStateException("Unsupported order event! [${event.activityType()}]")
        }
    }

    private suspend fun list(event: IndexerEvent, marks: EventTimeMarks) {
        val activity = event.history.activity as FlowNftOrderActivityList
        withSpan(
            name = "listOrderEvent",
            type = "event",
            labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")
        ) {
            orderService.enrichCancelList(activity.hash)
            val order = orderService.openList(activity, event.item)
            sendUpdate(order, marks)
        }
    }

    private suspend fun close(event: IndexerEvent, marks: EventTimeMarks) {
        val activity = event.history.activity as FlowNftOrderActivitySell
        return when (activity.left.asset) {
            is FlowAssetNFT -> orderClose(event, marks)
            is FlowAssetFungible -> acceptBid(event, marks)
            else -> throw IllegalStateException("Invalid order asset: ${activity.left.asset}, in activity: $activity")
        }
    }

    private suspend fun orderClose(event: IndexerEvent, marks: EventTimeMarks) {
        val activity = event.history.activity as FlowNftOrderActivitySell
        withSpan(
            "closeOrderEvent",
            type = "event",
            labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")
        ) {
            val order = orderService.close(activity)
            sendUpdate(order, marks)
            orderService.enrichTransfer(event.history.log.transactionHash, activity.left.maker, activity.right.maker)
                ?.also { sendHistoryUpdate(it) }
        }
    }

    private suspend fun orderCancelled(event: IndexerEvent, marks: EventTimeMarks) {
        val activity = event.history.activity as FlowNftOrderActivityCancelList
        val enrichedActivity = orderService.enrichCancelList(activity.hash)
        val order = orderService.cancel(activity, event.item)
        sendUpdate(order, marks)
        enrichedActivity?.let { sendHistoryUpdate(it) }
    }

    private suspend fun bid(event: IndexerEvent, marks: EventTimeMarks) {
        val activity = event.history.activity as FlowNftOrderActivityBid
        withSpan(
            "openBidEvent",
            type = "event",
            labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")
        ) {
            orderService.enrichCancelBid(activity.hash)
            val order = orderService.openBid(activity, event.item)
            sendUpdate(order, marks)
        }
    }

    private suspend fun acceptBid(event: IndexerEvent, marks: EventTimeMarks) {
        val activity = event.history.activity as FlowNftOrderActivitySell
        withSpan(
            "acceptBidEvent",
            type = "event",
            labels = listOf("itemId" to "${activity.contract}:${activity.tokenId}")
        ) {
            val order = orderService.closeBid(activity, event.item)
            sendUpdate(order, marks)
            orderService.enrichTransfer(event.history.log.transactionHash, activity.right.maker, activity.left.maker)
                ?.also { sendHistoryUpdate(it) }
        }
    }

    private suspend fun cancelBid(event: IndexerEvent, marks: EventTimeMarks) {
        val activity = event.history.activity as FlowNftOrderActivityCancelBid
        withSpan("cancelBidEvent", type = "event", labels = listOf("hash" to activity.hash)) {
            orderService.enrichCancelBid(activity.hash)
            val order = orderService.cancelBid(activity, event.item)
            sendUpdate(order, marks)
        }
    }

    private suspend fun sendHistoryUpdate(itemHistory: ItemHistory) {
        withSpan("sendOrderUpdate", "network") {
            protocolEventPublisher.activity(itemHistory)
        }
    }

    private suspend fun sendUpdate(order: Order, marks: EventTimeMarks) {
        withSpan("sendOrderUpdate", "network") {
            protocolEventPublisher.onOrderUpdate(order, orderConverter, marks)
        }
    }

}
