package com.rarible.flow.scanner.listener

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.nftco.flow.sdk.cadence.NumberField
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.StructField
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

interface ActivityMaker {

    fun isSupportedCollection(collection: String): Boolean

    suspend fun activities(events: List<FlowLogEvent>): List<BaseActivity>
}

abstract class NFTActivityMaker : ActivityMaker {

    abstract val contractName: String

    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()

    override fun isSupportedCollection(collection: String): Boolean =
        collection.split(".").last().lowercase() in arrayOf(contractName.lowercase())

    override suspend fun activities(events: List<FlowLogEvent>): List<BaseActivity> {
        val result: MutableList<BaseActivity> = mutableListOf()
        withSpan("generateNftActivities", "event") {
            val filtered = events.filterNot {
                when (it.type) {
                    FlowLogType.WITHDRAW -> cadenceParser.optional(it.event.fields["from"]!!) { value ->
                        address(value)
                    } == null
                    FlowLogType.DEPOSIT -> cadenceParser.optional(it.event.fields["to"]!!) { value ->
                        address(value)
                    } == null
                    else -> false
                }
            }

            val mintEvents = filtered.filter { it.type == FlowLogType.MINT }
            val withdrawEvents = filtered.filter { it.type == FlowLogType.WITHDRAW }
            val depositEvents = filtered.filter { it.type == FlowLogType.DEPOSIT }
            val burnEvents = filtered.filter { it.type == FlowLogType.BURN }

            mintEvents.forEach {
                val tokenId = tokenId(it)
                val deposit = depositEvents.first { d -> cadenceParser.long(d.event.fields["id"]!!) == tokenId }
                result.add(
                    MintActivity(
                        creator = creator(it),
                        owner = cadenceParser.optional(deposit.event.fields["to"]!!) { value ->
                            address(value)
                        }!!,
                        contract = it.event.eventId.collection(),
                        tokenId = tokenId,
                        timestamp = deposit.log.timestamp,
                        metadata = meta(it),
                        royalties = royalties(it)
                    )
                )
            }

            withdrawEvents.forEach { w ->
                val tokenId = tokenId(w)
                val from: OptionalField by w.event.fields
                val depositActivity = depositEvents.find { d ->
                    val dTokenId = cadenceParser.long(d.event.fields["id"]!!)
                    dTokenId == tokenId && d.log.timestamp >= w.log.timestamp
                }

                if (depositActivity != null) {
                    val to: OptionalField by depositActivity.event.fields
                    result.add(
                        TransferActivity(
                            contract = w.event.eventId.collection(),
                            tokenId = tokenId,
                            timestamp = depositActivity.log.timestamp,
                            from = cadenceParser.optional(from) {
                                address(it)
                            }!!,
                            to = cadenceParser.optional(to) {
                                address(it)
                            }!!
                        )
                    )
                } else {
                    val burnActivity = burnEvents.find { b ->
                        val bTokenId = cadenceParser.long(b.event.fields["id"]!!)
                        bTokenId == tokenId && b.log.timestamp >= w.log.timestamp
                    }
                    if (burnActivity != null) {
                        result.add(
                            BurnActivity(
                                contract = burnActivity.event.eventId.collection(),
                                tokenId = tokenId,
                                owner = cadenceParser.optional(from) {
                                    address(it)
                                },
                                timestamp = burnActivity.log.timestamp
                            )
                        )
                    }
                }
            }
        }
        return result.sortedBy { it.timestamp }.toList()
    }

    abstract fun tokenId(logEvent: FlowLogEvent): Long

    abstract fun meta(logEvent: FlowLogEvent): Map<String, String>

    protected open fun royalties(logEvent: FlowLogEvent): List<Part> = emptyList()

    protected open fun creator(logEvent: FlowLogEvent): String = logEvent.event.eventId.contractAddress.formatted
}

@Component
class TopShotActivityMaker(
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId
) : NFTActivityMaker() {
    override val contractName: String = "TopShot"

    private val royaltyAddress = mapOf(
        FlowChainId.MAINNET to FlowAddress("0xbd69b6abdfcf4539"),
        FlowChainId.TESTNET to FlowAddress("0x01658d9b94068f3c"),
    )

    override fun tokenId(logEvent: FlowLogEvent): Long = when (logEvent.type) {
        FlowLogType.MINT -> cadenceParser.long(logEvent.event.fields["momentID"]!!)
        else -> cadenceParser.long(logEvent.event.fields["id"]!!)
    }

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val msg = logEvent.event
        val playID: NumberField by msg.fields
        val setID: NumberField by msg.fields
        val serialNumber: NumberField by msg.fields
        return mapOf(
            "playID" to playID.value.toString(),
            "setID" to setID.value.toString(),
            "serialNumber" to serialNumber.value.toString()
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return listOf(Part(address = royaltyAddress[chainId]!!, fee = 0.05))
    }
}

@Component
class MotoGPActivityMaker : NFTActivityMaker() {

    override val contractName: String = "MotoGPCard"

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()
}

@Component
class EvolutionActivityMaker : NFTActivityMaker() {
    override val contractName: String = "Evolution"

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = mapOf(
        "itemId" to "${cadenceParser.int(logEvent.event.fields["itemId"]!!)}",
        "setId" to "${cadenceParser.int(logEvent.event.fields["setId"]!!)}",
        "serialNumber" to "${cadenceParser.int(logEvent.event.fields["serialNumber"]!!)}"
    )

}

@Component
class RaribleNFTActivityMaker : NFTActivityMaker() {
    override val contractName: String = "RaribleNFT"

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        return try {
            cadenceParser.dictionaryMap(logEvent.event.fields["metadata"]!!) { key, value ->
                string(key) to string(value)
            }
        } catch (_: Exception) {
            mapOf("metaURI" to cadenceParser.string(logEvent.event.fields["metadata"]!!))
        }
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return cadenceParser.arrayValues(logEvent.event.fields["royalties"]!!) {
            it as StructField
            Part(
                address = FlowAddress(address(it.value!!.getRequiredField("address"))),
                fee = double(it.value!!.getRequiredField("fee"))
            )
        }
    }

    override fun creator(logEvent: FlowLogEvent): String {
        return cadenceParser.address(logEvent.event.fields["creator"]!!)
    }
}

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
        return withSpan("readEventsFromOrderTx", "event") {
            txManager.onTransaction(
                blockHeight = blockHeight,
                transactionId = txId
            ) {
                it.events.map { Flow.unmarshall(EventMessage::class, it.event) }
            }
        }
    }

    protected suspend fun usdRate(contract: String, timestamp: Long): BigDecimal? = try {
        currencyApi.getCurrencyRate(BlockchainDto.FLOW, contract, timestamp).awaitSingle().rate
    } catch (e: Exception) {
        logger.warn("Unable to fetch USD price rate from currency api: ${e.message}", e)
        null
    }

    protected fun paymentType(address: String): PaymentType {
        return pTypes[chainId]!!.firstNotNullOfOrNull { if (it.value == address) it.key else null } ?: PaymentType.OTHER
    }

    private val pTypes: Map<FlowChainId, Map<PaymentType, String>> = mapOf(
        FlowChainId.MAINNET to mapOf(
            PaymentType.ROYALTY to "0xbd69b6abdfcf4539",
            PaymentType.BUYER_FEE to "0x7f599d6dd7fd7e7b",
            PaymentType.SELLER_FEE to "0x7f599d6dd7fd7e7b",
            PaymentType.OTHER to "0xf919ee77447b7497"
        ),
        FlowChainId.TESTNET to mapOf(
            PaymentType.BUYER_FEE to "0xebf4ae01d1284af8",
            PaymentType.SELLER_FEE to "0xebf4ae01d1284af8",
            PaymentType.OTHER to "0x912d5440f7e3769e"
        )
    )
}

@Component
class NFTStorefrontActivityMaker : OrderActivityMaker() {

    override val contractName: String = "NFTStorefront"

    override suspend fun activities(events: List<FlowLogEvent>): List<BaseActivity> {
        val result: MutableList<BaseActivity> = mutableListOf()
        withSpan("generateOrderActivities", "event") {
            val orderCancel =
                events.filter { it.type == FlowLogType.LISTING_COMPLETED && !cadenceParser.boolean(it.event.fields["purchased"]!!) }
            val orderPurchased =
                events.filter { it.type == FlowLogType.LISTING_COMPLETED && cadenceParser.boolean(it.event.fields["purchased"]!!) }
            val orderListed = events.filter { it.type == FlowLogType.LISTING_AVAILABLE }

            result.addAll(
                orderCancel.map {
                    val orderId = cadenceParser.long(it.event.fields["listingResourceID"]!!)
                    FlowNftOrderActivityCancelList(
                        hash = orderId.toString(),
                        timestamp = it.log.timestamp
                    )
                }
            )

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
                result.add(
                    FlowNftOrderActivityList(
                        price = price,
                        priceUsd = priceUsd,
                        tokenId = tokenId,
                        contract = nftCollection,
                        timestamp = it.log.timestamp,
                        hash = orderId.toString(),
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

                    val payInfo = currencyEvents.filter { it.eventId.eventName == "TokensDeposited" }.mapNotNull {
                        val to = cadenceParser.optional(it.fields["to"]!!) {
                            address(it)
                        } ?: return@mapNotNull null

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
                    result.add(
                        FlowNftOrderActivitySell(
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
                    )
                }
            }
        }
        return result.toList()
    }
}

@Component
class RaribleOpenBidActivityMaker(
    val flowBalanceService: FlowBalanceService
) : OrderActivityMaker() {
    override val contractName: String = "RaribleOpenBid"

    override suspend fun activities(events: List<FlowLogEvent>): List<BaseActivity> {
        val result: MutableList<BaseActivity> = mutableListOf()
        withSpan("generateOrderActivities", "event") {
            val canceledBids =
                events.filter { it.type == FlowLogType.BID_COMPLETED && !cadenceParser.boolean(it.event.fields["purchased"]!!) }
            val acceptedBids =
                events.filter { it.type == FlowLogType.BID_COMPLETED && cadenceParser.boolean(it.event.fields["purchased"]!!) }
            val openedBids = events.filter { it.type == FlowLogType.BID_AVAILABLE }

            result.addAll(
                canceledBids.map {
                    val orderId = cadenceParser.long(it.event.fields["bidId"]!!)
                    FlowNftOrderActivityCancelBid(
                        hash = orderId.toString(),
                        timestamp = it.log.timestamp
                    )
                }
            )

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
                result.add(
                    FlowNftOrderActivityBid(
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
                )

                flowBalanceService.initBalances(FlowAddress(maker), currencyContract)
            }

            acceptedBids.forEach {
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

                    val payInfo = payInfos(currencyEvents, sellerAddress)

                    val price = payInfo.filterNot {
                        it.type == PaymentType.OTHER
                    }.sumOf { it.amount }
                    val usdRate =
                        usdRate(payInfo.first().currencyContract, it.log.timestamp.toEpochMilli()) ?: BigDecimal.ZERO

                    val priceUsd = if (usdRate > BigDecimal.ZERO) {
                        price * usdRate
                    } else BigDecimal.ZERO
                    val tokenId = cadenceParser.long(withdrawnEvent.fields["id"]!!)
                    val hash = cadenceParser.long(it.event.fields["bidId"]!!).toString()
                    result.add(
                        FlowNftOrderActivityBidAccept(
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
                                    rate = BigDecimal.ZERO
                                )
                            }
                        )
                    )
                }
            }
        }
        return result.toList()
    }

    private fun payInfos(
        currencyEvents: List<EventMessage>,
        sellerAddress: String
    ): List<PayInfo> {
        try {
            val payInfo = currencyEvents.filter { it.eventId.eventName == "TokensDeposited" }.mapNotNull {
                val to = cadenceParser.optional(it.fields["to"]!!) {
                    address(it)
                } ?: return@mapNotNull null

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
