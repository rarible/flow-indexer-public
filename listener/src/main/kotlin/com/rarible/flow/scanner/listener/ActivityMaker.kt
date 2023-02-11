package com.rarible.flow.scanner.listener

//import com.nftco.flow.sdk.AddressRegistry.Companion.FLOW_FEES
//import com.nftco.flow.sdk.Flow
//import com.nftco.flow.sdk.FlowAddress
//import com.nftco.flow.sdk.FlowChainId
//import com.nftco.flow.sdk.FlowId
//import com.nftco.flow.sdk.cadence.JsonCadenceParser
//import com.nftco.flow.sdk.cadence.OptionalField
//import com.rarible.blockchain.scanner.flow.model.FlowLog
//import com.rarible.core.apm.withSpan
//import com.rarible.flow.core.domain.BaseActivity
//import com.rarible.flow.core.domain.BurnActivity
//import com.rarible.flow.core.domain.FlowAssetFungible
//import com.rarible.flow.core.domain.FlowAssetNFT
//import com.rarible.flow.core.domain.FlowLogEvent
//import com.rarible.flow.core.domain.FlowLogType
//import com.rarible.flow.core.domain.FlowNftOrderActivityBid
//import com.rarible.flow.core.domain.FlowNftOrderActivityCancelBid
//import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
//import com.rarible.flow.core.domain.FlowNftOrderActivityList
//import com.rarible.flow.core.domain.FlowNftOrderActivitySell
//import com.rarible.flow.core.domain.FlowNftOrderPayment
//import com.rarible.flow.core.domain.MintActivity
//import com.rarible.flow.core.domain.OrderActivityMatchSide
//import com.rarible.flow.core.domain.Part
//import com.rarible.flow.core.domain.PaymentType
//import com.rarible.flow.core.domain.TransferActivity
//import com.rarible.flow.core.repository.ItemCollectionRepository
//import com.rarible.flow.events.EventId
//import com.rarible.flow.events.EventMessage
//import com.rarible.flow.log.Log
//import com.rarible.flow.scanner.TxManager
//import com.rarible.flow.scanner.service.balance.FlowBalanceService
//import com.rarible.protocol.currency.api.client.CurrencyControllerApi
//import com.rarible.protocol.currency.dto.BlockchainDto
//import com.rarible.protocol.dto.FlowOrderPlatformDto
//import java.math.BigDecimal
//import kotlinx.coroutines.flow.toList
//import kotlinx.coroutines.reactive.asFlow
//import kotlinx.coroutines.reactor.awaitSingle
//import kotlinx.coroutines.runBlocking
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.stereotype.Component
//
//interface ActivityMaker {
//
//    fun isSupportedCollection(collection: String): Boolean
//
//    suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity>
//}
//
//abstract class NFTActivityMaker : ActivityMaker {
//
//    abstract val contractName: String
//
//    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()
//
//    fun <T> parse(fn: JsonCadenceParser.() -> T): T {
//        return fn(cadenceParser)
//    }
//
//    override fun isSupportedCollection(collection: String): Boolean =
//        collection.split(".").last().lowercase() == contractName.lowercase()
//
//    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
//        val result: MutableMap<FlowLog, BaseActivity> = mutableMapOf()
//        withSpan("generateNftActivities", "event") {
//            val filtered = events.filterNot {
//                when (it.type) {
//                    FlowLogType.WITHDRAW -> cadenceParser.optional(it.event.fields["from"]!!) { value ->
//                        address(value)
//                    } == null
//                    FlowLogType.DEPOSIT -> cadenceParser.optional(it.event.fields["to"]!!) { value ->
//                        address(value)
//                    } == null
//                    else -> false
//                }
//            }
//
//            val mintEvents = filtered.filter { it.type == FlowLogType.MINT }
//            val withdrawEvents = filtered.filter { it.type == FlowLogType.WITHDRAW }
//            val depositEvents = filtered.filter { it.type == FlowLogType.DEPOSIT }
//            val burnEvents = filtered.filter { it.type == FlowLogType.BURN }
//            val usedDeposit = mutableSetOf<FlowLogEvent>()
//
//            mintEvents.forEach {
//                val tokenId = tokenId(it)
//                val owner = depositEvents
//                    .firstOrNull { d -> cadenceParser.long(d.event.fields["id"]!!) == tokenId }
//                    ?.also(usedDeposit::add)
//                    ?.let { d ->
//                        cadenceParser.optional(d.event.fields["to"]!!) { value ->
//                            address(value)
//                        }
//                    }
//                    ?: it.event.eventId.contractAddress.formatted
//                result[it.log] = MintActivity(
//                    creator = creator(it),
//                    owner = owner,
//                    contract = it.event.eventId.collection(),
//                    tokenId = tokenId,
//                    timestamp = it.log.timestamp,
//                    metadata = meta(it),
//                    royalties = royalties(it)
//                )
//            }
//
//            withdrawEvents.forEach { w ->
//                val tokenId = tokenId(w)
//                val from: OptionalField by w.event.fields
//                val depositActivity = depositEvents.find { d ->
//                    val dTokenId = cadenceParser.long(d.event.fields["id"]!!)
//                    dTokenId == tokenId && d.log.timestamp >= w.log.timestamp
//                }?.also(usedDeposit::add)
//
//                if (depositActivity != null) {
//                    val to: OptionalField by depositActivity.event.fields
//                    result[depositActivity.log] = TransferActivity(
//                        contract = w.event.eventId.collection(),
//                        tokenId = tokenId,
//                        timestamp = depositActivity.log.timestamp,
//                        from = cadenceParser.optional(from) {
//                            address(it)
//                        }!!,
//                        to = cadenceParser.optional(to) {
//                            address(it)
//                        }!!,
//                        purchased = false,
//                    )
//                } else {
//                    val burnActivity = burnEvents.find { b ->
//                        val bTokenId = cadenceParser.long(b.event.fields["id"]!!)
//                        bTokenId == tokenId && b.log.timestamp >= w.log.timestamp
//                    }
//                    if (burnActivity != null) {
//                        result[burnActivity.log] = BurnActivity(
//                            contract = burnActivity.event.eventId.collection(),
//                            tokenId = tokenId,
//                            owner = cadenceParser.optional(from) {
//                                address(it)
//                            },
//                            timestamp = burnActivity.log.timestamp
//                        )
//                    } else {
//                        result[w.log] = TransferActivity(
//                            contract = w.event.eventId.collection(),
//                            tokenId = tokenId,
//                            timestamp = w.log.timestamp,
//                            from = cadenceParser.optional(from) {
//                                address(it)
//                            }!!,
//                            to = w.event.eventId.contractAddress.formatted,
//                            purchased = false,
//                        )
//                    }
//                }
//            }
//
//            (depositEvents - usedDeposit).forEach { d ->
//                val to: OptionalField by d.event.fields
//                result[d.log] = TransferActivity(
//                    contract = d.event.eventId.collection(),
//                    tokenId = tokenId(d),
//                    timestamp = d.log.timestamp,
//                    from = d.event.eventId.contractAddress.formatted,
//                    to = cadenceParser.optional(to) { address(it) }!!,
//                    purchased = false,
//                )
//            }
//        }
//        return result.toMap()
//    }
//
//    abstract fun tokenId(logEvent: FlowLogEvent): Long
//
//    abstract fun meta(logEvent: FlowLogEvent): Map<String, String>
//
//    protected open fun royalties(logEvent: FlowLogEvent): List<Part> = emptyList()
//
//    protected open fun creator(logEvent: FlowLogEvent): String = logEvent.event.eventId.contractAddress.formatted
//}
//
//abstract class OrderActivityMaker : ActivityMaker {
//
//    abstract val contractName: String
//
//    protected val nftCollectionEvents: Set<String> by lazy {
//        runBlocking {
//            collectionRepository.findAll().asFlow().toList().flatMap { listOf("${it.id}.Withdraw", "${it.id}.Deposit") }
//                .toSet()
//        }
//    }
//
//    protected val currenciesEvents: Set<String> by lazy {
//        currencies[chainId]!!.flatMap { listOf("${it}.TokensDeposited", "${it}.TokensWithdrawn") }.toSet()
//    }
//
//    private val logger by Log()
//
//    @Value("\${blockchain.scanner.flow.chainId}")
//    protected lateinit var chainId: FlowChainId
//
//    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()
//
//    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
//    @Autowired
//    protected lateinit var collectionRepository: ItemCollectionRepository
//
//    @Autowired
//    protected lateinit var txManager: TxManager
//
//    @Autowired
//    private lateinit var currencyApi: CurrencyControllerApi
//
//    private val currencies = mapOf(
//        FlowChainId.MAINNET to setOf("A.1654653399040a61.FlowToken", "A.3c5959b568896393.FUSD"),
//        FlowChainId.TESTNET to setOf("A.7e60df042a9c0868.FlowToken", "A.e223d8a629e49c68.FUSD"),
//        FlowChainId.EMULATOR to setOf("A.7e60df042a9c0868.FlowToken", "A.9a0766d93b6608b7.FUSD")
//    )
//
//    override fun isSupportedCollection(collection: String): Boolean =
//        EventId.of("${collection}.dummy").contractName.lowercase() == contractName.lowercase()
//
//    protected suspend fun readEvents(blockHeight: Long, txId: FlowId): List<EventMessage> {
//        return withSpan("readEventsFromOrderTx", "network") {
//            txManager.onTransaction(
//                blockHeight = blockHeight,
//                transactionId = txId
//            ) { transactionResult ->
//                transactionResult.events.map { Flow.unmarshall(EventMessage::class, it.event) }
//            }
//        }
//    }
//
//    protected suspend fun usdRate(contract: String, timestamp: Long): BigDecimal? = withSpan("usdRate", "network") {
//        try {
//            currencyApi.getCurrencyRate(BlockchainDto.FLOW, contract, timestamp).awaitSingle().rate
//        } catch (e: Exception) {
//            logger.warn("Unable to fetch USD price rate from currency api: ${e.message}", e)
//            null
//        }
//    }
//
//    protected fun paymentType(address: String): PaymentType {
//        return pTypes[chainId]!!.firstNotNullOfOrNull { if (it.value == address) it.key else null } ?: PaymentType.OTHER
//    }
//
//    protected fun payInfos(
//        currencyEvents: List<EventMessage>,
//        sellerAddress: String,
//    ): List<PayInfo> {
//        try {
//            val payments = currencyEvents.filter { it.eventId.eventName == "TokensDeposited" }
//                .filter { msg ->
//                    val a = cadenceParser.optional(msg.fields["to"]!!) {
//                        address(it)
//                    }
//                    a != null && a != Flow.DEFAULT_ADDRESS_REGISTRY.addressOf(FLOW_FEES, chainId)!!.formatted
//                }
//
//
//            var feeFounded = false
//            val payInfo = payments
//                .map { msg ->
//                    val to = cadenceParser.optional(msg.fields["to"]!!) {
//                        address(it)
//                    }!!
//
//                    val amount = cadenceParser.bigDecimal(msg.fields["amount"]!!)
//
//                    var type = paymentType(to)
//                    if (type == PaymentType.OTHER && to == sellerAddress) {
//                        type = PaymentType.REWARD
//                    } else if (type == PaymentType.BUYER_FEE) {
//                        if (!feeFounded) {
//                            feeFounded = true
//                        } else {
//                            type = PaymentType.SELLER_FEE
//                        }
//                    }
//                    PayInfo(
//                        address = to,
//                        amount = amount,
//                        currencyContract = msg.eventId.collection(),
//                        type = type
//                    )
//                }
//            return payInfo
//        } catch (e: Exception) {
//            throw e
//        }
//    }
//
//    private val pTypes: Map<FlowChainId, Map<PaymentType, String>> = mapOf(
//        FlowChainId.MAINNET to mapOf(
//            PaymentType.ROYALTY to "0xbd69b6abdfcf4539",
//            PaymentType.BUYER_FEE to "0x7f599d6dd7fd7e7b",
//            PaymentType.OTHER to "0xf919ee77447b7497"
//        ),
//        FlowChainId.TESTNET to mapOf(
//            PaymentType.BUYER_FEE to "0xebf4ae01d1284af8",
//            PaymentType.OTHER to "0x912d5440f7e3769e"
//        )
//    )
//}
//
//@Component
//class NFTStorefrontActivityMaker : OrderActivityMaker() {
//
//    override val contractName: String = "NFTStorefront"
//
//    private val raribleOrderAddress = mapOf(
//        FlowChainId.MAINNET to "01ab36aaf654a13e",
//        FlowChainId.TESTNET to "ebf4ae01d1284af8"
//    )
//
//    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
//        val result: MutableMap<FlowLog, BaseActivity> = mutableMapOf()
//        withSpan("generateOrderActivities", "event") {
//            val orderCancel =
//                events.filter { it.type == FlowLogType.LISTING_COMPLETED && !cadenceParser.boolean(it.event.fields["purchased"]!!) }
//            val orderPurchased =
//                events.filter { it.type == FlowLogType.LISTING_COMPLETED && cadenceParser.boolean(it.event.fields["purchased"]!!) }
//            val orderListed = events.filter { it.type == FlowLogType.LISTING_AVAILABLE }
//
//            orderCancel.forEach {
//                val orderId = cadenceParser.long(it.event.fields["listingResourceID"]!!)
//                result[it.log] = FlowNftOrderActivityCancelList(
//                    hash = "$orderId",
//                    timestamp = it.log.timestamp
//                )
//            }
//
//            orderListed.forEach {
//                val raribleEventPrice = checkRaribleEventPrice(it)
//                val price = raribleEventPrice ?: cadenceParser.bigDecimal(it.event.fields["price"]!!)
//                val orderId = cadenceParser.long(it.event.fields["listingResourceID"]!!)
//                val rate = usdRate(
//                    EventId.of(cadenceParser.type(it.event.fields["ftVaultType"]!!)).collection(),
//                    it.log.timestamp.toEpochMilli()
//                ) ?: BigDecimal.ZERO
//
//                val priceUsd = if (rate > BigDecimal.ZERO) {
//                    price * rate
//                } else BigDecimal.ZERO
//                val nftCollection = EventId.of(cadenceParser.type(it.event.fields["nftType"]!!)).collection()
//                val tokenId = cadenceParser.long(it.event.fields["nftID"]!!)
//
//                result[it.log] = FlowNftOrderActivityList(
//                    price = price,
//                    priceUsd = priceUsd,
//                    tokenId = tokenId,
//                    contract = nftCollection,
//                    timestamp = it.log.timestamp,
//                    hash = "$orderId",
//                    maker = cadenceParser.address(it.event.fields["storefrontAddress"]!!),
//                    make = FlowAssetNFT(
//                        contract = nftCollection,
//                        value = BigDecimal.ONE,
//                        tokenId = tokenId
//                    ),
//                    take = FlowAssetFungible(
//                        contract = EventId.of(cadenceParser.type(it.event.fields["ftVaultType"]!!)).collection(),
//                        value = price
//                    )
//                )
//            }
//            orderPurchased.forEach { event ->
//                val allTxEvents =
//                    readEvents(blockHeight = event.log.blockHeight, txId = FlowId(event.log.transactionHash))
//                val tokenEvents = allTxEvents.filter { it.eventId.toString() in nftCollectionEvents }
//                val currencyEvents = allTxEvents.filter { it.eventId.toString() in currenciesEvents }
//
//                if (tokenEvents.isNotEmpty() && currencyEvents.isNotEmpty()) {
//                    val withdrawnEvent = tokenEvents.find { it.eventId.eventName == "Withdraw" }!!
//                    val depositEvent = tokenEvents.find { it.eventId.eventName == "Deposit" }!!
//
//                    val buyerAddress = cadenceParser.optional(depositEvent.fields["to"]!!) {
//                        address(it)
//                    }!!
//                    val sellerAddress = cadenceParser.optional(withdrawnEvent.fields["from"]!!) {
//                        address(it)
//                    }!!
//
//                    val payInfo = payInfos(currencyEvents, sellerAddress)
//                    val price = payInfo.filterNot {
//                        it.type == PaymentType.BUYER_FEE
//                    }.sumOf { it.amount }
//                    val usdRate =
//                        usdRate(payInfo.first().currencyContract, event.log.timestamp.toEpochMilli()) ?: BigDecimal.ZERO
//                    val priceUsd = if (usdRate > BigDecimal.ZERO) {
//                        price * usdRate
//                    } else BigDecimal.ZERO
//                    val tokenId = cadenceParser.long(withdrawnEvent.fields["id"]!!)
//                    val hash = cadenceParser.long(event.event.fields["listingResourceID"]!!).toString()
//                    result[event.log] = FlowNftOrderActivitySell(
//                        price = price,
//                        priceUsd = priceUsd,
//                        tokenId = tokenId,
//                        contract = withdrawnEvent.eventId.collection(),
//                        hash = hash,
//                        left = OrderActivityMatchSide(
//                            maker = sellerAddress,
//                            asset = FlowAssetNFT(
//                                contract = withdrawnEvent.eventId.collection(),
//                                tokenId = tokenId,
//                                value = BigDecimal.ONE
//                            )
//                        ),
//                        right = OrderActivityMatchSide(
//                            maker = buyerAddress,
//                            asset = FlowAssetFungible(
//                                contract = payInfo.first().currencyContract,
//                                value = price
//                            )
//                        ),
//                        timestamp = event.log.timestamp,
//                        payments = payInfo.map {
//                            FlowNftOrderPayment(
//                                type = it.type,
//                                address = it.address,
//                                amount = it.amount,
//                                rate = BigDecimal.valueOf((it.amount.toDouble() / price.toDouble()) * 100.0)
//                            )
//                        },
//                        platform = FlowOrderPlatformDto.RARIBLE
//                    )
//                }
//            }
//        }
//        return result.toMap()
//    }
//
//    private suspend fun checkRaribleEventPrice(event: FlowLogEvent): BigDecimal? {
//        val eventName = "A.${raribleOrderAddress[chainId]}.RaribleOrder.OrderAvailable"
//        return txManager.onTransaction(
//            blockHeight = event.log.blockHeight,
//            transactionId = FlowId(event.log.transactionHash)
//        ) { result ->
//            val e = result.events.find { it.type == eventName }
//            if (e != null) {
//                return@onTransaction cadenceParser.bigDecimal(e.event["price"]!!)
//            }
//            return@onTransaction null
//        }
//    }
//}
//
//@Component
//class RaribleOpenBidActivityMaker(
//    val flowBalanceService: FlowBalanceService,
//) : OrderActivityMaker() {
//    override val contractName: String = "RaribleOpenBid"
//
//    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
//        val result: MutableMap<FlowLog, BaseActivity> = mutableMapOf()
//        withSpan("generateOrderActivities", "event") {
//            val canceledBids =
//                events.filter { it.type == FlowLogType.BID_COMPLETED && !cadenceParser.boolean(it.event.fields["purchased"]!!) }
//            val acceptedBids =
//                events.filter { it.type == FlowLogType.BID_COMPLETED && cadenceParser.boolean(it.event.fields["purchased"]!!) }
//            val openedBids = events.filter { it.type == FlowLogType.BID_AVAILABLE }
//
//            canceledBids.forEach {
//                val orderId = cadenceParser.long(it.event.fields["bidId"]!!)
//                result[it.log] = FlowNftOrderActivityCancelBid(
//                    hash = orderId.toString(),
//                    timestamp = it.log.timestamp
//                )
//
//            }
//
//            openedBids.forEach {
//                val price = cadenceParser.bigDecimal(it.event.fields["bidPrice"]!!)
//                val orderId = cadenceParser.long(it.event.fields["bidId"]!!)
//                val currencyContract = EventId.of(cadenceParser.type(it.event.fields["vaultType"]!!)).collection()
//                val usdRate = usdRate(currencyContract, it.log.timestamp.toEpochMilli()) ?: BigDecimal.ZERO
//
//                val priceUsd = if (usdRate > BigDecimal.ZERO) {
//                    price * usdRate
//                } else BigDecimal.ZERO
//                val nftCollection = EventId.of(cadenceParser.type(it.event.fields["nftType"]!!)).collection()
//                val tokenId = cadenceParser.long(it.event.fields["nftId"]!!)
//                val maker = cadenceParser.address(it.event.fields["bidAddress"]!!)
//                result[it.log] = FlowNftOrderActivityBid(
//                    price = price,
//                    priceUsd = priceUsd,
//                    tokenId = tokenId,
//                    contract = nftCollection,
//                    timestamp = it.log.timestamp,
//                    hash = orderId.toString(),
//                    maker = maker,
//                    make = FlowAssetFungible(
//                        contract = currencyContract,
//                        value = price
//                    ),
//                    take = FlowAssetNFT(
//                        contract = nftCollection,
//                        value = BigDecimal.ONE,
//                        tokenId = tokenId
//                    ),
//                )
//
//                flowBalanceService.initBalances(FlowAddress(maker), currencyContract)
//            }
//
//            acceptedBids.forEach { flowLogEvent ->
//                val allTxEvents = readEvents(blockHeight = flowLogEvent.log.blockHeight,
//                    txId = FlowId(flowLogEvent.log.transactionHash))
//                val tokenEvents = allTxEvents.filter { it.eventId.toString() in nftCollectionEvents }
//                val currencyEvents = allTxEvents.filter { it.eventId.toString() in currenciesEvents }
//
//                if (tokenEvents.isNotEmpty() && currencyEvents.isNotEmpty()) {
//                    val withdrawnEvent = tokenEvents.find { it.eventId.eventName == "Withdraw" }!!
//                    val depositEvent = tokenEvents.find { it.eventId.eventName == "Deposit" }!!
//
//                    val buyerAddress = cadenceParser.optional(depositEvent.fields["to"]!!) {
//                        address(it)
//                    }!!
//                    val sellerAddress = cadenceParser.optional(withdrawnEvent.fields["from"]!!) {
//                        address(it)
//                    }!!
//
//                    val payInfo = payInfos(currencyEvents, sellerAddress)
//
//                    val price = payInfo.filterNot {
//                        it.type == PaymentType.BUYER_FEE
//                    }.sumOf { it.amount }
//                    val usdRate =
//                        usdRate(payInfo.first().currencyContract, flowLogEvent.log.timestamp.toEpochMilli())
//                            ?: BigDecimal.ZERO
//
//                    val priceUsd = if (usdRate > BigDecimal.ZERO) {
//                        price * usdRate
//                    } else BigDecimal.ZERO
//                    val tokenId = cadenceParser.long(withdrawnEvent.fields["id"]!!)
//                    val hash = cadenceParser.long(flowLogEvent.event.fields["bidId"]!!).toString()
//                    result[flowLogEvent.log] = FlowNftOrderActivitySell(
//                        price = price,
//                        priceUsd = priceUsd,
//                        tokenId = tokenId,
//                        contract = withdrawnEvent.eventId.collection(),
//                        hash = hash,
//                        right = OrderActivityMatchSide(
//                            maker = sellerAddress,
//                            asset = FlowAssetNFT(
//                                contract = withdrawnEvent.eventId.collection(),
//                                tokenId = tokenId,
//                                value = BigDecimal.ONE
//                            )
//                        ),
//                        left = OrderActivityMatchSide(
//                            maker = buyerAddress,
//                            asset = FlowAssetFungible(
//                                contract = payInfo.first().currencyContract,
//                                value = price
//                            )
//                        ),
//                        timestamp = flowLogEvent.log.timestamp,
//                        payments = payInfo.map {
//                            FlowNftOrderPayment(
//                                type = it.type,
//                                address = it.address,
//                                amount = it.amount,
//                                rate = BigDecimal.ZERO
//                            )
//                        },
//                        platform = FlowOrderPlatformDto.RARIBLE
//                    )
//                }
//            }
//        }
//        return result.toMap()
//    }
//}
//
//data class PayInfo(
//    val address: String,
//    val amount: BigDecimal,
//    val currencyContract: String,
//    val type: PaymentType,
//)
