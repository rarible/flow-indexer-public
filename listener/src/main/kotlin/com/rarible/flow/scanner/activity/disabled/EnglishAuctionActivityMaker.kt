package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.NumberField
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.AuctionActivityBidClosed
import com.rarible.flow.core.domain.AuctionActivityBidIncreased
import com.rarible.flow.core.domain.AuctionActivityBidOpened
import com.rarible.flow.core.domain.AuctionActivityLot
import com.rarible.flow.core.domain.AuctionActivityLotCanceled
import com.rarible.flow.core.domain.AuctionActivityLotCleaned
import com.rarible.flow.core.domain.AuctionActivityLotEndTimeChanged
import com.rarible.flow.core.domain.AuctionActivityLotHammered
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.activity.order.WithPaymentsActivityMaker
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class EnglishAuctionActivityMaker : WithPaymentsActivityMaker() {

    override val contractName: String = "EnglishAuction"
    override fun getItemId(event: FlowLogEvent): ItemId? {
        return null
    }

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        try {
            return events.map {
                val activity = when (it.type) {
                    FlowLogType.LOT_AVAILABLE -> lotAvailableActivity(it)
                    FlowLogType.OPEN_BID -> openBidActivity(it)
                    FlowLogType.LOT_END_TIME_CHANGED -> changeEndTime(it)
                    FlowLogType.CLOSE_BID -> closeBidActivity(it)
                    FlowLogType.LOT_COMPLETED -> if (cadenceParser.boolean(it.event.fields["isCancelled"]!!)) {
                        lotCanceledActivity(it)
                    } else lotHammeredActivity(it)
                    FlowLogType.INCREASE_BID -> bidIncreased(it)
                    FlowLogType.LOT_CLEANED -> lotFinalizedActivity(it)
                    else -> throw IllegalStateException("Unsupported event type: ${it.type}")
                }
                it.log to activity
            }.associate { it.first to it.second }
        } catch (e: Exception) {
            logger.error("EnglishAuctionActivityMaker::activities failed! ${e.message}", e)
            throw Throwable(e)
        }
    }

    private fun bidIncreased(event: FlowLogEvent): BaseActivity {
        val lotId by event.event.fields
        val bidder by event.event.fields
        val amount by event.event.fields

        return AuctionActivityBidIncreased(
            lotId = cadenceParser.long(lotId),
            bidder = cadenceParser.address(bidder),
            amount = cadenceParser.bigDecimal(amount),
            timestamp = event.log.timestamp
        )
    }

    private fun changeEndTime(flowLogEvent: FlowLogEvent): BaseActivity {
        val lotId by flowLogEvent.event.fields
        val finishAt by flowLogEvent.event.fields
        return AuctionActivityLotEndTimeChanged(
            lotId = cadenceParser.long(lotId),
            finishAt = resolveTimeAt(finishAt)!!,
            timestamp = flowLogEvent.log.timestamp
        )
    }

    private fun closeBidActivity(event: FlowLogEvent): BaseActivity {
        val lotId by event.event.fields
        val bidder by event.event.fields
        val isWinner by event.event.fields
        return AuctionActivityBidClosed(
            lotId = cadenceParser.long(lotId),
            bidder = cadenceParser.address(bidder),
            isWinner = cadenceParser.boolean(isWinner),
            timestamp = event.log.timestamp
        )
    }

    private fun openBidActivity(event: FlowLogEvent): BaseActivity {
        val lotId by event.event.fields
        val bidder by event.event.fields
        val amount by event.event.fields
        return AuctionActivityBidOpened(
            lotId = cadenceParser.long(lotId),
            bidder = cadenceParser.address(bidder),
            amount = cadenceParser.bigDecimal(amount),
            timestamp = event.log.timestamp
        )
    }

    private fun lotFinalizedActivity(event: FlowLogEvent): BaseActivity {
        val lotId by event.event.fields
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

        val sellerAddress = if (event.event.fields["seller"] != null) {
            cadenceParser.address(event.event.fields["seller"]!!)
        } else ""
        val payInfos = payInfos(currencyEvents, sellerAddress)
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

    private fun lotCanceledActivity(event: FlowLogEvent): BaseActivity = with(cadenceParser) {
        AuctionActivityLotCanceled(
            lotId = long(event.event.fields["lotId"]!!),
            timestamp = event.log.timestamp
        )
    }

    private fun lotAvailableActivity(event: FlowLogEvent): BaseActivity {
        try {
            val lotId by event.event.fields
            val seller by event.event.fields.withDefault { AddressField("0x00") }
            val itemType by event.event.fields
            val itemId by event.event.fields
            val bidType by event.event.fields
            val increment by event.event.fields
            val minimumBid by event.event.fields
            val buyoutPrice by event.event.fields
            val startAt by event.event.fields
            val finishAt by event.event.fields
            val duration by event.event.fields.withDefault { NumberField("UFix64", "0.0") }
            return AuctionActivityLot(
                lotId = cadenceParser.long(lotId),
                contract = EventId.of(cadenceParser.string(itemType)).collection(),
                tokenId = cadenceParser.long(itemId),
                timestamp = event.log.timestamp,
                currency = cadenceParser.string(bidType),
                minStep = cadenceParser.bigDecimal(increment),
                startPrice = cadenceParser.bigDecimal(minimumBid),
                buyoutPrice = cadenceParser.optional(buyoutPrice) { bigDecimal(it) },
                startAt = resolveTimeAt(startAt)!!,
                finishAt = resolveTimeAt(finishAt),
                duration = cadenceParser.bigDecimal(duration).longValueExact(),
                seller = cadenceParser.address(seller)
            )
        } catch (e: Exception) {
            throw e
        }
    }

    private fun resolveTimeAt(timeField: Field<*>): Instant? = when (timeField) {
        is OptionalField -> cadenceParser.optional(timeField) {
            Instant.ofEpochSecond(bigDecimal(it).longValueExact())
        }
        is NumberField -> Instant.ofEpochSecond(
            cadenceParser.bigDecimal(timeField).longValueExact()
        )
        else -> null
    }
}
