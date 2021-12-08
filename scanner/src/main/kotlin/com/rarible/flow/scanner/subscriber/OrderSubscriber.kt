package com.rarible.flow.scanner.subscriber
//
//import com.nftco.flow.sdk.FlowChainId
//import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
//import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
//import com.rarible.flow.core.domain.*
//import com.rarible.flow.events.EventMessage
//import com.rarible.flow.scanner.cadence.OrderAvailable
//import com.rarible.flow.scanner.cadence.OrderCancelled
//import com.rarible.flow.scanner.cadence.OrderClosed
//import com.rarible.flow.scanner.model.parse
//import org.springframework.stereotype.Component
//import java.math.BigDecimal
//import java.time.Instant
//
//@Component
//class OrderSubscriber : BaseFlowLogEventSubscriber() {
//
//    private val events = "OrderAvailable,OrderClosed,OrderCancelled".split(",")
//
//    override val descriptors = mapOf(
//        FlowChainId.MAINNET to flowDescriptor(
//            address = "01ab36aaf654a13e",
//            contract = "RaribleOrder",
//            events = events,
//            startFrom = 19863151L
//        ),
//        FlowChainId.TESTNET to flowDescriptor(
//            address = "ebf4ae01d1284af8",
//            contract = "RaribleOrder",
//            events = events,
//            startFrom = 49754786L
//        ),
//        FlowChainId.EMULATOR to flowDescriptor(
//            address = "f8d6e0586b0a20c7",
//            contract = "RaribleOrder",
//            events = events,
//        ),
//    )
//
//    override suspend fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): BaseActivity? {
//        val timestamp = Instant.ofEpochMilli(block.timestamp)
//
//        return when (msg.eventId.eventName) {
//            "OrderAvailable" -> {
//                val event = log.event.parse<OrderAvailable>()
//                logger.debug("OrderSubscriber: $event")
//
//                FlowNftOrderActivityList(
//                    contract = event.nftType.collection(),
//                    tokenId = event.nftId,
//                    timestamp = timestamp,
//                    price = event.price,
//                    priceUsd = event.price * usdRate(event.vaultType.collection(), block.timestamp),
//                    make = FlowAssetNFT(
//                        contract = event.nftType.collection(),
//                        value = BigDecimal.ONE,
//                        tokenId = event.nftId
//                    ),
//                    take = FlowAssetFungible(
//                        contract = event.vaultType.collection(),
//                        value = event.price
//                    ),
//                    hash = event.orderId.toString(),
//                    maker = event.orderAddress.formatted,
//                    payments = event.payments.map { FlowNftOrderPayment(it.type, it.address, it.rate, it.amount) }
//                )
//            }
//
//            "OrderClosed" -> {
//                val event = log.event.parse<OrderClosed>()
//                logger.debug("OrderSubscriber: $event")
//
//                val orderPrice = event.price - event.cuts[0].rate
//                FlowNftOrderActivitySell(
//                    contract = event.nftType.collection(),
//                    tokenId = event.nftId,
//                    timestamp = timestamp,
//                    price = orderPrice,
//                    priceUsd = event.price * usdRate(event.vaultType.collection(), block.timestamp),
//                    left = OrderActivityMatchSide(event.orderAddress,
//                        FlowAssetNFT(event.nftType.collection(), BigDecimal.ONE, event.nftId)),
//                    right = OrderActivityMatchSide(event.buyerAddress,
//                        FlowAssetFungible(event.vaultType.collection(), orderPrice)),
//                    hash = event.orderId.toString(),
//                )
//            }
//
//            "OrderCancelled" -> {
//                val event = log.event.parse<OrderCancelled>()
//                logger.debug("OrderSubscriber: $event")
//
//                val orderPrice = event.price - event.cuts[0].rate
//                FlowNftOrderActivityCancelList(
//                    contract = event.nftType.collection(),
//                    tokenId = event.nftId,
//                    timestamp = timestamp,
//                    price = orderPrice,
//                    priceUsd = event.price * usdRate(event.vaultType.collection(), block.timestamp),
//                    make = FlowAssetNFT(
//                        contract = event.nftType.collection(),
//                        value = BigDecimal.ONE,
//                        tokenId = event.nftId
//                    ),
//                    take = FlowAssetFungible(
//                        contract = event.vaultType.collection(),
//                        value = orderPrice
//                    ),
//                    hash = event.orderId.toString(),
//                    maker = event.orderAddress,
//                )
//            }
//
//            else -> throw IllegalStateException("Unsupported eventId: ${msg.eventId}")
//        }
//    }
//}
