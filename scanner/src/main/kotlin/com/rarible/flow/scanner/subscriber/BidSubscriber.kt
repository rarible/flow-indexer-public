package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.*
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.flow.core.domain.*
import com.rarible.flow.events.EventMessage
import com.rarible.flow.scanner.cadence.BidAvailable
import com.rarible.flow.scanner.cadence.BidCompleted
import com.rarible.flow.scanner.model.parse
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class BidSubscriber(
    private val sporkService: SporkService,
): BaseItemHistoryFlowLogSubscriber() {
    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.TESTNET to flowDescriptor(
                address = "",
                contract = "",
                events = listOf()
            )
        )

    override suspend fun activity(
        block: FlowBlockchainBlock,
        log: FlowBlockchainLog,
        msg: EventMessage
    ): BaseActivity? {
        val timestamp = Instant.ofEpochMilli(block.timestamp)
        return when(msg.eventId.eventName) {
            "BidAvailable" -> {
                val event = log.event.parse<BidAvailable>()
                val payments = calcPayments(
                    itemId = ItemId(contract = event.nftType.collection(), tokenId = event.nftId),
                    saleCuts = event.cuts,
                    price = event.price
                )

                FlowNftOrderActivityBid(
                    price = event.price,
                    priceUsd = usdRate(event.vaultType.collection(), block.timestamp),
                    hash = event.bidId.toString(),
                    tokenId = event.nftId,
                    contract = event.nftType.collection(),
                    timestamp = timestamp,
                    maker = event.bidAddress.formatted,
                    make = FlowAssetFungible(
                        contract = event.vaultType.collection(),
                        value = event.price
                    ),
                    take = FlowAssetNFT(
                        contract = event.nftType.collection(),
                        value = BigDecimal.ONE,
                        tokenId = event.nftId
                    ),
                    payments = payments
                )
            }
            "BidCompleted" -> {
                val event = log.event.parse<BidCompleted>()
                val builder = ScriptBuilder()
                builder.script = FlowScript(
                    Flow.DEFAULT_ADDRESS_REGISTRY.processScript(
                        script = """
                        //todo prepare script
                    """.trimIndent(),
                        chainId = chainId
                    )
                )

                val details = sporkService.spork(block.number).api.executeScriptAtBlockHeight(
                    script = FlowScript(""),
                    height = block.number,
                    arguments = listOf()
                ).await().jsonCadence
                if (event.purchased) {
                    FlowNftOrderActivitySell(
//                        price = order.amount,
//                        priceUsd = usdRate(order.collection, block.timestamp),
//                        tokenId = order.itemId.tokenId,
//                        contract = order.collection,
//                        timestamp = timestamp,
//                        hash = event.bidId.toString(),
//                        left = OrderActivityMatchSide(
//                            maker = order.maker.formatted,
//                            asset = order.make
//                        ),
//                        right = OrderActivityMatchSide(
//                            maker =
//                        )
                    )
                } else {
                    FlowNftOrderActivityCancelBid(

                    )
                }
            }
            else -> throw IllegalStateException("Unsupported event ${msg.eventId.eventName}")
        }
    }

    private fun calcPayments(itemId: ItemId, saleCuts: Map<FlowAddress, BigDecimal>, price: BigDecimal): List<FlowNftOrderPayment> {
        TODO("Not yet implemented")
    }
}
