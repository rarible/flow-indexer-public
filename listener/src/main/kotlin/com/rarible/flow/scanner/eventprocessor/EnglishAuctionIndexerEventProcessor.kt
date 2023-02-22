package com.rarible.flow.scanner.eventprocessor

import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.EnglishAuctionService
import org.springframework.stereotype.Component

@Component
class EnglishAuctionIndexerEventProcessor(
    private val englishAuctionService: EnglishAuctionService,
    private val protocolEventPublisher: ProtocolEventPublisher
) : IndexerEventsProcessor {

    private val supportedTypes = setOf(
        FlowActivityType.LOT_AVAILABLE,
        FlowActivityType.LOT_COMPLETED,
        FlowActivityType.LOT_END_TIME_CHANGED,
        FlowActivityType.LOT_CLEANED,
        FlowActivityType.LOT_CANCELED,
        FlowActivityType.OPEN_BID,
        FlowActivityType.CLOSE_BID,
        FlowActivityType.INCREASE_BID
    )

    override fun isSupported(event: IndexerEvent): Boolean = event.activityType() in supportedTypes

    override suspend fun process(event: IndexerEvent) {
        when(event.activityType()) {
            FlowActivityType.LOT_AVAILABLE -> openLot(event)
            FlowActivityType.LOT_COMPLETED -> completeLot(event)
            FlowActivityType.LOT_END_TIME_CHANGED -> changeLotEndTime(event)
            FlowActivityType.LOT_CLEANED -> cleanLot(event)
            FlowActivityType.LOT_CANCELED -> cancelLot(event)
            FlowActivityType.OPEN_BID -> openBid(event)
            FlowActivityType.CLOSE_BID -> {/** do nothing */}
            FlowActivityType.INCREASE_BID -> increaseBid(event)
            else -> throw IllegalStateException("Unsupported activity type [${event.activityType()}]")
        }
    }

    private suspend fun increaseBid(event: IndexerEvent) {
        val activity = event.history.activity as AuctionActivityBidIncreased
        withSpan("increaseBid") {
            sendKafka(event, englishAuctionService.increaseBid(activity))

        }
    }

    private suspend fun cancelLot(event: IndexerEvent) {
        val activity = event.history.activity as AuctionActivityLotCanceled
        withSpan("cancelLot") {
            sendKafka(event, englishAuctionService.cancelLot(activity))
        }
    }

    private suspend fun openBid(event: IndexerEvent) {
        val activity = event.history.activity as AuctionActivityBidOpened
        withSpan("openBid") {
            sendKafka(event, englishAuctionService.openBid(activity))
        }
    }

    private suspend fun cleanLot(event: IndexerEvent) {
        val activity = event.history.activity as AuctionActivityLotCleaned
        withSpan("cleanLot") {
            sendKafka(event, englishAuctionService.finalizeLot(activity))
        }
    }

    private suspend fun changeLotEndTime(event: IndexerEvent) {
        val activity = event.history.activity as AuctionActivityLotEndTimeChanged
        withSpan("changeLotEndTime") {
            sendKafka(event,englishAuctionService.changeLotEndTime(activity))
        }
    }

    private suspend fun completeLot(event: IndexerEvent) {
        val activity = event.history.activity as AuctionActivityLotHammered
        withSpan("completeLot") {
            sendKafka(event, englishAuctionService.hammerLot(activity))
        }
    }

    private suspend fun openLot(event: IndexerEvent) {
        val activity = event.history.activity as AuctionActivityLot
        withSpan("openLot", type = "event") {
            sendKafka(event, englishAuctionService.openLot(activity))
        }
    }

    private suspend fun sendKafka(event: IndexerEvent, lot: EnglishAuctionLot) {
        protocolEventPublisher.auction(lot)
    }
}
