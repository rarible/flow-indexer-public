package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Component(OrderClosedListener.ID)
class OrderClosedListener(
    private val itemService: ItemService,
    private val orderRepository: OrderRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        orderId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) = runBlocking {
        val itemId = ItemId(contract, orderId)
        val order = orderRepository
            .findActiveById(orderId)
            .awaitSingleOrNull()
        if (order?.taker != null) {
            itemService
                .transferNft(itemId, order.taker!!)
                ?.let { (item, ownership) ->
                    protocolEventPublisher.onItemUpdate(item)
                    protocolEventPublisher.onUpdate(ownership)

                    val fill = order.take?.value ?: BigDecimal.ZERO
                    val savedOrder = orderRepository.coSave(
                        order.copy(
                            fill = fill
                        )
                    )
                    protocolEventPublisher.onUpdate(savedOrder)

                    itemHistoryRepository.save(
                        ItemHistory(
                            id = UUID.randomUUID().toString(),
                            date = LocalDateTime.now(ZoneOffset.UTC),
                            activity = FlowNftOrderActivitySell(
                                price = order.take?.value ?: BigDecimal.ZERO,
                                left = OrderActivityMatchSide(
                                    order.maker, order.make
                                ),
                                right = OrderActivityMatchSide(
                                    order.taker!!, order.take!!
                                ),
                                blockHash = blockInfo.blockId,
                                blockNumber = blockInfo.blockHeight,
                                transactionHash = blockInfo.transactionId
                            )
                        )
                    )
                }

        }
    }


    companion object {
        const val ID = "RegularSaleOrder.OrderClosed"
    }
}
