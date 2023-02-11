package com.rarible.flow.scanner.activitymaker

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowId
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivityBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.FlowNftOrderPayment
import com.rarible.flow.core.domain.OrderActivityMatchSide
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.event.EventId
import com.rarible.flow.scanner.service.balance.FlowBalanceService
import java.math.BigDecimal
import org.springframework.stereotype.Component


@Component
class NFTStorefrontActivityMaker : WithPaymentsActivityMaker() {

    override val contractName: String = "NFTStorefront"

    private val raribleOrderAddress = mapOf(
        FlowChainId.MAINNET to "01ab36aaf654a13e",
        FlowChainId.TESTNET to "ebf4ae01d1284af8"
    )

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        val result: MutableMap<FlowLog, BaseActivity> = mutableMapOf()
        withSpan("generateOrderActivities", "event") {
            val orderCancel =
                events.filter { it.type == FlowLogType.LISTING_COMPLETED && !cadenceParser.boolean(it.event.fields["purchased"]!!) }
            val orderPurchased =
                events.filter { it.type == FlowLogType.LISTING_COMPLETED && cadenceParser.boolean(it.event.fields["purchased"]!!) }
            val orderListed = events.filter { it.type == FlowLogType.LISTING_AVAILABLE }

            orderCancel.forEach {
                val orderId = cadenceParser.long(it.event.fields["listingResourceID"]!!)
                result[it.log] = FlowNftOrderActivityCancelList(
                    hash = "$orderId",
                    timestamp = it.log.timestamp
                )
            }

            orderListed.forEach {
                val raribleEventPrice = checkRaribleEventPrice(it)
                val price = raribleEventPrice ?: cadenceParser.bigDecimal(it.event.fields["price"]!!)
                val orderId = cadenceParser.long(it.event.fields["listingResourceID"]!!)
                val rate = usdRate(
                    EventId.of(cadenceParser.string(it.event.fields["ftVaultType"]!!)).collection(), //was type
                    it.log.timestamp.toEpochMilli()
                ) ?: BigDecimal.ZERO

                val priceUsd = if (rate > BigDecimal.ZERO) {
                    price * rate
                } else BigDecimal.ZERO
                val nftCollection = EventId.of(cadenceParser.string(it.event.fields["nftType"]!!)).collection() //was type
                val tokenId = cadenceParser.long(it.event.fields["nftID"]!!)
                result[it.log] = FlowNftOrderActivityList(
                    price = price,
                    priceUsd = priceUsd,
                    tokenId = tokenId,
                    contract = nftCollection,
                    timestamp = it.log.timestamp,
                    hash = "$orderId",
                    maker = cadenceParser.address(it.event.fields["storefrontAddress"]!!),
                    make = FlowAssetNFT(
                        contract = nftCollection,
                        value = BigDecimal.ONE,
                        tokenId = tokenId
                    ),
                    take = FlowAssetFungible(
                        contract = EventId.of(cadenceParser.string(it.event.fields["ftVaultType"]!!)).collection(), //was type
                        value = price
                    )
                )
            }
            orderPurchased.forEach { logEvent ->
                val allTxEvents = readEvents(blockHeight = logEvent.log.blockHeight, txId = FlowId(logEvent.log.transactionHash))
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
                        usdRate(payInfo.first().currencyContract, logEvent.log.timestamp.toEpochMilli()) ?: BigDecimal.ZERO
                    val priceUsd = if (usdRate > BigDecimal.ZERO) {
                        price * usdRate
                    } else BigDecimal.ZERO
                    val tokenId = cadenceParser.long(withdrawnEvent.fields["id"]!!)
                    val hash = cadenceParser.long(logEvent.event.fields["listingResourceID"]!!).toString()
                    result[logEvent.log] = FlowNftOrderActivitySell(
                        price = price,
                        priceUsd = priceUsd,
                        tokenId = tokenId,
                        contract = withdrawnEvent.eventId.collection(),
                        hash = hash,
                        left = OrderActivityMatchSide(
                            maker = sellerAddress,
                            asset = FlowAssetNFT(
                                contract = withdrawnEvent.eventId.collection(),
                                tokenId = tokenId,
                                value = BigDecimal.ONE
                            )
                        ),
                        right = OrderActivityMatchSide(
                            maker = buyerAddress,
                            asset = FlowAssetFungible(
                                contract = payInfo.first().currencyContract,
                                value = price
                            )
                        ),
                        timestamp = logEvent.log.timestamp,
                        payments = payInfo.map {
                            FlowNftOrderPayment(
                                type = it.type,
                                address = it.address,
                                amount = it.amount,
                                rate = BigDecimal.valueOf((it.amount.toDouble() / price.toDouble()) * 100.0)
                            )
                        }
                    )
                }
            }
        }
        return result.toMap()
    }

    private suspend fun checkRaribleEventPrice(event: FlowLogEvent): BigDecimal? {
        val eventName = "A.${raribleOrderAddress[chainId]}.RaribleOrder.OrderAvailable"
        return txManager.onTransaction(
            blockHeight = event.log.blockHeight,
            transactionId = FlowId(event.log.transactionHash)
        ) { result ->
            val e = result.events.find { it.type == eventName }
            if (e != null) {
                return@onTransaction cadenceParser.bigDecimal(e.event["price"]!!)
            }
            return@onTransaction null
        }
    }
}

@Component
class RaribleOpenBidActivityMaker(
    val flowBalanceService: FlowBalanceService
) : WithPaymentsActivityMaker() {
    override val contractName: String = "RaribleOpenBid"

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        val result: MutableMap<FlowLog, BaseActivity> = mutableMapOf()
        withSpan("generateOrderActivities", "event") {
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
                val currencyContract = EventId.of(cadenceParser.string(it.event.fields["vaultType"]!!)).collection() //was type
                val usdRate = usdRate(currencyContract, it.log.timestamp.toEpochMilli()) ?: BigDecimal.ZERO

                val priceUsd = if (usdRate > BigDecimal.ZERO) {
                    price * usdRate
                } else BigDecimal.ZERO
                val nftCollection = EventId.of(cadenceParser.string(it.event.fields["nftType"]!!)).collection() //was type
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
                val allTxEvents = readEvents(blockHeight = flowLogEvent.log.blockHeight,
                    txId = FlowId(flowLogEvent.log.transactionHash))
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
        }
        return result.toMap()
    }



}


