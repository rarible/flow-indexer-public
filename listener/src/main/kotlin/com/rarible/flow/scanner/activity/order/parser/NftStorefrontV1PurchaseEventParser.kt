package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.AddressRegistry
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowId
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.FlowNftOrderPayment
import com.rarible.flow.core.domain.OrderActivityMatchSide
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.model.PayInfo
import com.rarible.flow.scanner.service.CurrencyService
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class NftStorefrontV1PurchaseEventParser(
    private val txManager: TxManager,
    currencyService: CurrencyService,
    supportedCollectionService: SupportedNftCollectionProvider,
    properties: FlowListenerProperties
) : AbstractNftStorefrontPurchaseEventParser(currencyService, supportedCollectionService) {

    private val chainId = properties.chainId
    private val nftCollectionEvents = supportedCollectionService.getEvents(properties.chainId)

    override suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivitySell? {
        val event = logEvent.event
        val log = logEvent.log

        val allTxEvents = readEvents(blockHeight = log.blockHeight, txId = FlowId(log.transactionHash))
        val tokenEvents = allTxEvents.filter { it.eventId.toString() in nftCollectionEvents }
        val currencyEvents = allTxEvents.filter { it.eventId.toString() in currenciesEvents }

        return if (tokenEvents.isNotEmpty() && currencyEvents.isNotEmpty()) {
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
                usdRate(it.currencyContract, logEvent.log.timestamp)
            } ?: BigDecimal.ZERO

            val priceUsd = if (usdRate > BigDecimal.ZERO) {
                price * usdRate
            } else BigDecimal.ZERO

            val tokenId = cadenceParser.long(withdrawnEvent.fields["id"]!!)
            val hash = cadenceParser.long(logEvent.event.fields["listingResourceID"]!!).toString()

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
        } else null
    }

    private val currenciesEvents: Set<String> by lazy {
        currencies[chainId]!!.flatMap { listOf("$it.TokensDeposited", "$it.TokensWithdrawn") }.toSet()
    }

    protected fun payInfos(
        currencyEvents: List<EventMessage>,
        sellerAddress: String
    ): List<PayInfo> {
        try {
            val payments = currencyEvents
                .filter {
                    it.eventId.eventName == "TokensDeposited"
                }
                .filter { msg ->
                    val address = cadenceParser.optional(msg.fields["to"]!!) {
                        address(it)
                    }
                    address != null && address != Flow.DEFAULT_ADDRESS_REGISTRY.addressOf(
                        AddressRegistry.FLOW_FEES,
                        chainId
                    )!!.formatted
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

    private fun paymentType(address: String): PaymentType {
        return pTypes[chainId]!!.firstNotNullOfOrNull { if (it.value == address) it.key else null } ?: PaymentType.OTHER
    }

    protected suspend fun readEvents(blockHeight: Long, txId: FlowId): List<EventMessage> {
        return txManager.onTransaction(
            blockHeight = blockHeight,
            transactionId = txId
        ) { transactionResult ->
            transactionResult.events.map { Flow.unmarshall(EventMessage::class, it.event) }
        }
    }

    private val currencies = mapOf(
        FlowChainId.MAINNET to setOf("A.1654653399040a61.FlowToken", "A.3c5959b568896393.FUSD"),
        FlowChainId.TESTNET to setOf("A.7e60df042a9c0868.FlowToken", "A.9a0766d93b6608b7.FUSD"),
        FlowChainId.EMULATOR to setOf("A.7e60df042a9c0868.FlowToken", "A.9a0766d93b6608b7.FUSD")
    )

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
