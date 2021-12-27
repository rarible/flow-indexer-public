package com.rarible.flow.scanner.activitymaker

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.service.balance.FlowBalanceService
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal

abstract class OrderActivityMaker : ActivityMaker {

    abstract val contractName: String

    protected val nftCollectionEvents: Set<String> by lazy {
        runBlocking {
            collectionRepository.findAll().asFlow().toList().flatMap { listOf("${it.id}.Withdraw", "${it.id}.Deposit") }
                .toSet()
        }
    }

    protected val currenciesEvents: Set<String> by lazy {
        currencies[chainId]!!.flatMap { listOf("${it}.TokensDeposited", "${it}.TokensWithdrawn") }.toSet()
    }

    private val logger by Log()

    @Value("\${blockchain.scanner.flow.chainId}")
    protected lateinit var chainId: FlowChainId

    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()

    @Autowired
    protected lateinit var collectionRepository: ItemCollectionRepository

    @Autowired
    protected lateinit var txManager: TxManager

    @Autowired
    private lateinit var currencyApi: CurrencyControllerApi

    private val currencies = mapOf(
        FlowChainId.MAINNET to setOf("A.1654653399040a61.FlowToken", "A.3c5959b568896393.FUSD"),
        FlowChainId.TESTNET to setOf("A.7e60df042a9c0868.FlowToken", "A.9a0766d93b6608b7.FUSD"),
        FlowChainId.EMULATOR to setOf("A.7e60df042a9c0868.FlowToken", "A.9a0766d93b6608b7.FUSD")
    )

    override fun isSupportedCollection(collection: String): Boolean =
        EventId.of("${collection}.dummy").contractName.lowercase() == contractName.lowercase()

    protected suspend fun readEvents(blockHeight: Long, txId: FlowId): List<EventMessage> {
        return withSpan("readEventsFromOrderTx", "network") {
            txManager.onTransaction(
                blockHeight = blockHeight,
                transactionId = txId
            ) { transactionResult ->
                transactionResult.events.map { Flow.unmarshall(EventMessage::class, it.event) }
            }
        }
    }

    protected suspend fun usdRate(contract: String, timestamp: Long): BigDecimal? = withSpan("usdRate", "network") {
        try {
            currencyApi.getCurrencyRate(BlockchainDto.FLOW, contract, timestamp).awaitSingle().rate
        } catch (e: Exception) {
            logger.warn("Unable to fetch USD price rate from currency api: ${e.message}", e)
            null
        }
    }

    protected fun paymentType(address: String): PaymentType {
        return pTypes[chainId]!!.firstNotNullOfOrNull { if (it.value == address) it.key else null } ?: PaymentType.OTHER
    }

    private val pTypes: Map<FlowChainId, Map<PaymentType, String>> = mapOf(
        FlowChainId.MAINNET to mapOf(
            PaymentType.ROYALTY to "0xbd69b6abdfcf4539",
            PaymentType.BUYER_FEE to "0x7f599d6dd7fd7e7b",
            PaymentType.OTHER to "0xf919ee77447b7497"
        ),
        FlowChainId.TESTNET to mapOf(
            PaymentType.BUYER_FEE to "0xebf4ae01d1284af8",
            PaymentType.OTHER to "0x912d5440f7e3769e"
        )
    )
}

@Component
class NFTStorefrontActivityMaker : OrderActivityMaker() {

    override val contractName: String = "NFTStorefront"

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
                val price = cadenceParser.bigDecimal(it.event.fields["price"]!!)
                val orderId = cadenceParser.long(it.event.fields["listingResourceID"]!!)
                val rate = usdRate(
                    EventId.of(cadenceParser.string(it.event.fields["ftVaultType"]!!)).collection(),
                    it.log.timestamp.toEpochMilli()
                ) ?: BigDecimal.ZERO

                val priceUsd = if (rate > BigDecimal.ZERO) {
                    price * rate
                } else BigDecimal.ZERO
                val nftCollection = EventId.of(cadenceParser.string(it.event.fields["nftType"]!!)).collection()
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
                        contract = EventId.of(cadenceParser.string(it.event.fields["ftVaultType"]!!)).collection(),
                        value = price
                    )
                )
            }
            orderPurchased.forEach {
                val allTxEvents = readEvents(blockHeight = it.log.blockHeight, txId = FlowId(it.log.transactionHash))
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

                    val payInfo = currencyEvents.filter { it.eventId.eventName == "TokensDeposited" }
                        .filter { cadenceParser.optional(it.fields["to"]!!) { address(it) } != null }
                        .map {
                            val to = cadenceParser.optional(it.fields["to"]!!) {
                                address(it)
                            }!!

                            val amount = cadenceParser.bigDecimal(it.fields["amount"]!!)

                            var type = paymentType(to)
                            if (type == PaymentType.OTHER && to == sellerAddress) {
                                type = PaymentType.REWARD
                            }
                            PayInfo(
                                address = to,
                                amount = amount,
                                currencyContract = it.eventId.collection(),
                                type = type
                            )
                        }

                    val price = payInfo.filterNot {
                        it.type == PaymentType.OTHER
                    }.sumOf { it.amount }
                    val usdRate =
                        usdRate(payInfo.first().currencyContract, it.log.timestamp.toEpochMilli()) ?: BigDecimal.ZERO
                    val priceUsd = if (usdRate > BigDecimal.ZERO) {
                        price * usdRate
                    } else BigDecimal.ZERO
                    val tokenId = cadenceParser.long(withdrawnEvent.fields["id"]!!)
                    val hash = cadenceParser.long(it.event.fields["listingResourceID"]!!).toString()
                    result[it.log] = FlowNftOrderActivitySell(
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
                        timestamp = it.log.timestamp,
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
}

@Component
class RaribleOpenBidActivityMaker(
    val flowBalanceService: FlowBalanceService
) : OrderActivityMaker() {
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
                val currencyContract = EventId.of(cadenceParser.string(it.event.fields["vaultType"]!!)).collection()
                val usdRate = usdRate(currencyContract, it.log.timestamp.toEpochMilli()) ?: BigDecimal.ZERO

                val priceUsd = if (usdRate > BigDecimal.ZERO) {
                    price * usdRate
                } else BigDecimal.ZERO
                val nftCollection = EventId.of(cadenceParser.string(it.event.fields["nftType"]!!)).collection()
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
                val allTxEvents = readEvents(blockHeight = flowLogEvent.log.blockHeight, txId = FlowId(flowLogEvent.log.transactionHash))
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
                        it.type in arrayOf(PaymentType.SELLER_FEE, PaymentType.OTHER)
                    }.sumOf { it.amount }
                    val usdRate =
                        usdRate(payInfo.first().currencyContract, flowLogEvent.log.timestamp.toEpochMilli()) ?: BigDecimal.ZERO

                    val priceUsd = if (usdRate > BigDecimal.ZERO) {
                        price * usdRate
                    } else BigDecimal.ZERO
                    val tokenId = cadenceParser.long(withdrawnEvent.fields["id"]!!)
                    val hash = cadenceParser.long(flowLogEvent.event.fields["bidId"]!!).toString()
                    result[flowLogEvent.log] = FlowNftOrderActivityBidAccept(
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

    private fun payInfos(
        currencyEvents: List<EventMessage>,
        sellerAddress: String
    ): List<PayInfo> {
        try {
            val payments = currencyEvents.filter { it.eventId.eventName == "TokensDeposited" }
                .filter { msg ->
                    cadenceParser.optional(msg.fields["to"]!!) {
                        address(it)
                    } != null
                }


            var feeFounded = false
            val payInfo = payments
                .map { msg ->
                    val to = cadenceParser.optional(msg.fields["to"]!!) {
                        address(it)
                    }!!

                    val amount = cadenceParser.bigDecimal(msg.fields["amount"]!!)

                    var type = paymentType(to)
                    if (type == PaymentType.OTHER && to == sellerAddress) {
                        type = PaymentType.REWARD
                    } else if (type == PaymentType.BUYER_FEE) {
                        if (!feeFounded) {
                            feeFounded = true
                        } else {
                            type = PaymentType.SELLER_FEE
                        }
                    }
                    PayInfo(
                        address = to,
                        amount = amount,
                        currencyContract = msg.eventId.collection(),
                        type = type
                    )
                }
            return payInfo
        } catch (e: Exception) {
            throw e
        }
    }

}

data class PayInfo(
    val address: String,
    val amount: BigDecimal,
    val currencyContract: String,
    val type: PaymentType
)
