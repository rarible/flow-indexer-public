package com.rarible.flow.scanner.activity.order

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowId
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.FlowNftOrderPayment
import com.rarible.flow.core.domain.OrderActivityMatchSide
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component
import java.math.BigDecimal

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
                    EventId.of(cadenceParser.type(it.event.fields["ftVaultType"]!!)).collection(),
                    it.log.timestamp.toEpochMilli()
                ) ?: BigDecimal.ZERO

                val priceUsd = if (rate > BigDecimal.ZERO) {
                    price * rate
                } else BigDecimal.ZERO
                val nftCollection = EventId.of(cadenceParser.type(it.event.fields["nftType"]!!)).collection()
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
                        contract = EventId.of(cadenceParser.type(it.event.fields["ftVaultType"]!!)).collection(),
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

                    val usdRate = payInfo.firstOrNull()?.let {
                        usdRate(it.currencyContract, logEvent.log.timestamp.toEpochMilli())
                    } ?: BigDecimal.ZERO

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


