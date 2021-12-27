package com.rarible.flow.scanner.activitymaker

import com.nftco.flow.sdk.cadence.*
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.*
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EnglishAuctionActivityMaker : ActivityMaker {

    private val contractName = "EnglishAuction"

    private val cadenceParser = JsonCadenceParser()

    override fun isSupportedCollection(collection: String): Boolean =
        collection.lowercase().endsWith(contractName.lowercase())

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        val lotOpened = events.filter { it.type == FlowLogType.LOT_AVAILABLE }.associate { it.log to lotAvailableActivity(it) }
        val bidOpened = events.filter { it.type == FlowLogType.OPEN_BID }.associate { it.log to openBidActivity(it) }
        val bidClosed = events.filter { it.type == FlowLogType.CLOSE_BID }.associate { it.log to closeBidActivity(it) }
        val lotHammered = events.filter { it.type == FlowLogType.LOT_COMPLETED && !cadenceParser.boolean(it.event.fields["isCanceled"]!!) }.associate { it.log to lotHammeredActivity(it) }
        val lotCleaned = events.filter { it.type == FlowLogType.LOT_CLEANED }.associate { it.log to lotFinalizedActivity(it) }
        return lotOpened + bidOpened + bidClosed + lotHammered + lotCleaned
    }

    private fun closeBidActivity(event: FlowLogEvent): BaseActivity {
        val lotId: NumberField by event.event.fields
        val bidder: AddressField by event.event.fields
        val isWinner: BooleanField by event.event.fields
        return AuctionActivityBidClosed(
            lotId = cadenceParser.long(lotId),
            bidder = cadenceParser.address(bidder),
            isWinner = cadenceParser.boolean(isWinner),
            timestamp = event.log.timestamp
        )
    }

    private fun openBidActivity(event: FlowLogEvent): BaseActivity {
        val lotId: NumberField by event.event.fields
        val bidder: AddressField by event.event.fields
        val amount: NumberField by event.event.fields
        return AuctionActivityBidOpened(
            lotId = cadenceParser.long(lotId),
            bidder = cadenceParser.address(bidder),
            amount = cadenceParser.bigDecimal(amount),
            timestamp = event.log.timestamp
        )
    }

    private fun lotFinalizedActivity(event: FlowLogEvent): BaseActivity {
        val lotId: NumberField by event.event.fields
        return AuctionActivityLotCleaned(
            lotId = cadenceParser.long(lotId),
            timestamp = event.log.timestamp
        )
    }

    private fun lotHammeredActivity(event: FlowLogEvent): BaseActivity {
        TODO("Not yet implemented")
    }

    private fun lotCanceledActivity(event: FlowLogEvent): BaseActivity {
        TODO("Not yet implemented")
    }

    private fun lotAvailableActivity(event: FlowLogEvent): BaseActivity {
        val lotId: NumberField by event.event.fields
        val itemType: StringField by event.event.fields
        val itemId: NumberField by event.event.fields
        val bidType: StringField by event.event.fields
        val increment: NumberField by event.event.fields
        val minimumBid: NumberField by event.event.fields
        val buyoutPrice: OptionalField by event.event.fields
        val startAt: NumberField by event.event.fields
        val finishAt: NumberField by event.event.fields
        return AuctionActivityLot(
            lotId = cadenceParser.long(lotId),
            contract = cadenceParser.string(itemType),
            tokenId = cadenceParser.long(itemId),
            timestamp = event.log.timestamp,
            currency = cadenceParser.string(bidType),
            minStep = cadenceParser.bigDecimal(increment),
            startPrice = cadenceParser.bigDecimal(minimumBid),
            buyoutPrice = cadenceParser.optional(buyoutPrice) { bigDecimal(it) },
            startAt = Instant.ofEpochSecond(cadenceParser.bigDecimal(startAt).longValueExact()),
            finishAt = Instant.ofEpochSecond(cadenceParser.bigDecimal(finishAt).longValueExact()),
            duration = -1L, //todo read duration from event
            seller = "0x00" //todo read seller from event
        )
    }
}
