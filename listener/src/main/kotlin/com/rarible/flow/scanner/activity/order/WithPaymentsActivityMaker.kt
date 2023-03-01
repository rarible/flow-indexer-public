package com.rarible.flow.scanner.activity.order

import com.nftco.flow.sdk.AddressRegistry.Companion.FLOW_FEES
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.PaymentType
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.core.util.Log
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.ActivityMaker
import com.rarible.flow.scanner.model.PayInfo
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.math.BigDecimal

abstract class WithPaymentsActivityMaker : ActivityMaker {

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

    protected val logger by Log()

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
        collection.substringAfterLast(".").lowercase() == contractName.lowercase()

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
            withTimeout(10_000L) {
                currencyApi.getCurrencyRate(BlockchainDto.FLOW, contract, timestamp).awaitSingle().rate
            }
        } catch (e: Exception) {
            logger.warn("Unable to fetch USD price rate from currency api: ${e.message}", e)
            null
        }
    }

    protected fun paymentType(address: String): PaymentType {
        return pTypes[chainId]!!.firstNotNullOfOrNull { if (it.value == address) it.key else null } ?: PaymentType.OTHER
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
                    address != null && address != Flow.DEFAULT_ADDRESS_REGISTRY.addressOf(FLOW_FEES, chainId)!!.formatted
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

