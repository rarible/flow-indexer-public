package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Component(OrderAssigned.ID)
class OrderAssigned(
    private val orderRepository: OrderRepository,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val itemRepository: ItemRepository
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        orderId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) = runBlocking<Unit> {
        orderRepository
            .findActiveById(orderId)
            .awaitSingleOrNull()
            ?.let { order ->
                val taker = FlowAddress(fields["to"]!! as String)
                orderRepository.coSave(order.copy(taker = taker))
                val item = itemRepository.coFindById(order.itemId)!!
                itemHistoryRepository.coSave(
                    ItemHistory(
                        id = UUID.randomUUID().toString(),
                        date = LocalDateTime.now(ZoneOffset.UTC),
                        activity = FlowNftOrderActivitySell(
                            price = order.amount,
                            left = OrderActivityMatchSide(
                                maker = order.maker,
                                asset = FlowAssetNFT(
                                    contract = item.contract,
                                    value = BigDecimal.valueOf(1L),
                                    tokenId = order.itemId.tokenId
                                )
                            ),
                            right = OrderActivityMatchSide(
                                maker = taker,
                                asset = FlowAssetFungible(
                                    contract = FlowAddress("0x1654653399040a61"), //TODO get from config
                                    value = order.amount
                                )
                            ),
                            transactionHash = blockInfo.transactionId,
                            blockNumber = blockInfo.blockHeight,
                            blockHash = blockInfo.blockId
                        )
                    )
                )
            }


    }

    companion object {
        const val ID = "StoreShowCase.OrderAssigned"
    }
}
