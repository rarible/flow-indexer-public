package com.rarible.flow.scanner.activitymaker

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.cadence.*
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.*
import com.rarible.flow.log.Log
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class EnglishAuctionActivityMaker : WithPaymentsActivityMaker() {

    private val logger by Log()

    override val contractName: String = "EnglishAuction"

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        try {
            val lotOpened =
                events.filter { it.type == FlowLogType.LOT_AVAILABLE }.associate { it.log to lotAvailableActivity(it) }
            val bidOpened =
                events.filter { it.type == FlowLogType.OPEN_BID }.associate { it.log to openBidActivity(it) }
            val changeTime =
                events.filter { it.type == FlowLogType.LOT_END_TIME_CHANGED }.associate { it.log to changeEndTime(it) }
            val bidClosed =
                events.filter { it.type == FlowLogType.CLOSE_BID }.associate { it.log to closeBidActivity(it) }
            val lotCanceled =
                events.filter { it.type == FlowLogType.LOT_COMPLETED && cadenceParser.boolean(it.event.fields["isCancelled"]!!) }
                    .associate { it.log to lotCanceledActivity(it) }
            val lotHammered =
                events.filter { it.type == FlowLogType.LOT_COMPLETED && !cadenceParser.boolean(it.event.fields["isCancelled"]!!) }
                    .associate { it.log to lotHammeredActivity(it) }
            val lotCleaned =
                events.filter { it.type == FlowLogType.LOT_CLEANED }.associate { it.log to lotFinalizedActivity(it) }
            return (lotOpened + bidOpened + changeTime + bidClosed + lotCanceled + lotHammered + lotCleaned)
                .toSortedMap(compareBy { it.eventIndex })
        } catch (e: Exception) {
            logger.error("EnglishAuctionActivityMaker::activities failed! ${e.message}", e)
            throw Throwable(e)
        }
    }

    private fun changeEndTime(flowLogEvent: FlowLogEvent): BaseActivity {
        val lotId: NumberField by flowLogEvent.event.fields
        val finishAt: NumberField by flowLogEvent.event.fields
        return AuctionActivityLotEndTimeChanged(
            lotId = cadenceParser.long(lotId),
            finishAt = Instant.ofEpochSecond(cadenceParser.double(finishAt).toLong()),
            timestamp = flowLogEvent.log.timestamp
        )
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

    private suspend fun lotHammeredActivity(event: FlowLogEvent): BaseActivity {
        val lotId = cadenceParser.long(event.event.fields["lotId"]!!)
        val bidderAddress =
            cadenceParser.optional(event.event.fields["bidder"]!!) { address(it) }
                ?: throw IllegalStateException("Bidder address not defined!")
        val hammerPrice =
            cadenceParser.optional(event.event.fields["hammerPrice"]!!) { bigDecimal(it) }
                ?: throw IllegalStateException("Hammer price not defined!")

        val txEvents = readEvents(event.log.blockHeight, FlowId(event.log.transactionHash))

        val depositNft =
            txEvents.filter { "${it.eventId}" in nftCollectionEvents }.first { it.eventId.eventName == "Deposit" }
        val currencyEvents = txEvents.filter { "${it.eventId}" in currenciesEvents }

        val payInfos = payInfos(currencyEvents, "") //todo add seller address
        return AuctionActivityLotHammered(
            lotId = lotId,
            contract = depositNft.eventId.collection(),
            tokenId = cadenceParser.long(depositNft.fields["id"]!!),
            winner = FlowAddress(bidderAddress),
            hammerPrice = hammerPrice,
            hammerPriceUsd = usdRate(payInfos.first().currencyContract, event.log.timestamp.toEpochMilli()).let {
                if (it == null) BigDecimal.ZERO else hammerPrice * it
            },
            timestamp = event.log.timestamp,
            payments = payInfos.filter { it.type in setOf(PaymentType.REWARD, PaymentType.ROYALTY) }
                .map { Payout(FlowAddress(it.address), it.amount) },
            originFees = payInfos.filter { it.type in setOf(PaymentType.BUYER_FEE, PaymentType.SELLER_FEE) }
                .map { Payout(FlowAddress(it.address), it.amount) }
        )
    }

    private fun lotCanceledActivity(event: FlowLogEvent): BaseActivity = AuctionActivityLotCanceled(
        lotId = cadenceParser.long(event.event.fields["lotId"]!!),
        timestamp = event.log.timestamp
    )

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
