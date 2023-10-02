package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowId
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivityBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelBid
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.FlowNftOrderPayment
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.OrderActivityMatchSide
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.activity.order.WithPaymentsActivityMaker
import com.rarible.flow.scanner.service.balance.FlowBalanceService
import java.math.BigDecimal

class RaribleOpenBidActivityMaker(
    private val flowBalanceService: FlowBalanceService
) : WithPaymentsActivityMaker() {

    override val contractName: String = "RaribleOpenBid"
    override fun getItemId(event: FlowLogEvent): ItemId? {
        return null
    }

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        val result: MutableMap<FlowLog, BaseActivity> = mutableMapOf()
        val canceledBids =
            events.filter { it.type == FlowLogType.BID_COMPLETED && !cadenceParser.boolean(it.event.fields["purchased"]!!) }
        val acceptedBids =
            events.filter { it.type == FlowLogType.BID_COMPLETED && cadenceParser.boolean(it.event.fields["purchased"]!!) }
        val openedBids = events.filter { it.type == FlowLogType.BID_AVAILABLE }

        canceledBids.forEach {
            val orderId = cadenceParser.long(it.event.fields["bidId"]!!)
            result[it.log] = FlowNftOrderActivityCancelBid(
                hash = orderId.toString(),
                timestamp = it.log.timestamp
            )

        }

        openedBids.forEach {
            val price = cadenceParser.bigDecimal(it.event.fields["bidPrice"]!!)
            val orderId = cadenceParser.long(it.event.fields["bidId"]!!)
            val currencyContract = EventId.of(cadenceParser.type(it.event.fields["vaultType"]!!)).collection()
            val usdRate = usdRate(currencyContract, it.log.timestamp.toEpochMilli()) ?: BigDecimal.ZERO

            val priceUsd = if (usdRate > BigDecimal.ZERO) {
                price * usdRate
            } else BigDecimal.ZERO
            val nftCollection = EventId.of(cadenceParser.type(it.event.fields["nftType"]!!)).collection()
            val tokenId = cadenceParser.long(it.event.fields["nftId"]!!)
            val maker = cadenceParser.address(it.event.fields["bidAddress"]!!)
            result[it.log] = FlowNftOrderActivityBid(
                price = price,
                priceUsd = priceUsd,
                tokenId = tokenId,
                contract = nftCollection,
                timestamp = it.log.timestamp,
                hash = orderId.toString(),
                maker = maker,
                make = FlowAssetFungible(
                    contract = currencyContract,
                    value = price
                ),
                take = FlowAssetNFT(
                    contract = nftCollection,
                    value = BigDecimal.ONE,
                    tokenId = tokenId
                ),
            )

            flowBalanceService.initBalances(FlowAddress(maker), currencyContract)
        }

        acceptedBids.forEach { flowLogEvent ->
            val allTxEvents = readEvents(
                blockHeight = flowLogEvent.log.blockHeight,
                txId = FlowId(flowLogEvent.log.transactionHash)
            )
            val tokenEvents = allTxEvents.filter { it.eventId.toString() in nftCollectionEvents }
            val currencyEvents = allTxEvents.filter { it.eventId.toString() in currenciesEvents }

            if (tokenEvents.isNotEmpty() && currencyEvents.isNotEmpty()) {
                val withdrawnEvent = tokenEvents.find { it.eventId.eventName == "Withdraw" }!!
                val depositEvent = tokenEvents.find { it.eventId.eventName == "Deposit" }!!

                val buyerAddress = cadenceParser.optional(depositEvent.fields["to"]!!) {
                    address(it)
                }!!
                val sellerAddress = cadenceParser.optional(withdrawnEvent.fields["from"]!!) {
                    address(it)
                }!!

                val payInfo = payInfos(currencyEvents, sellerAddress)

                val price = payInfo.filterNot {
                    it.type == PaymentType.BUYER_FEE
                }.sumOf { it.amount }
                val usdRate =
                    usdRate(payInfo.first().currencyContract, flowLogEvent.log.timestamp.toEpochMilli())
                        ?: BigDecimal.ZERO

                val priceUsd = if (usdRate > BigDecimal.ZERO) {
                    price * usdRate
                } else BigDecimal.ZERO
                val tokenId = cadenceParser.long(withdrawnEvent.fields["id"]!!)
                val hash = cadenceParser.long(flowLogEvent.event.fields["bidId"]!!).toString()
                result[flowLogEvent.log] = FlowNftOrderActivitySell(
                    price = price,
                    priceUsd = priceUsd,
                    tokenId = tokenId,
                    contract = withdrawnEvent.eventId.collection(),
                    hash = hash,
                    right = OrderActivityMatchSide(
                        maker = sellerAddress,
                        asset = FlowAssetNFT(
                            contract = withdrawnEvent.eventId.collection(),
                            tokenId = tokenId,
                            value = BigDecimal.ONE
                        )
                    ),
                    left = OrderActivityMatchSide(
                        maker = buyerAddress,
                        asset = FlowAssetFungible(
                            contract = payInfo.first().currencyContract,
                            value = price
                        )
                    ),
                    timestamp = flowLogEvent.log.timestamp,
                    payments = payInfo.map {
                        FlowNftOrderPayment(
                            type = it.type,
                            address = it.address,
                            amount = it.amount,
                            rate = BigDecimal.ZERO
                        )
                    }
                )
            }
        }
        return result.toMap()
    }
}