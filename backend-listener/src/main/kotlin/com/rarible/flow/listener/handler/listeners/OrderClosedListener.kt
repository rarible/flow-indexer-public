package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component(OrderClosedListener.ID)
class OrderClosedListener(
    private val orderRepository: OrderRepository,
    private val itemRepository: ItemRepository,
    private val protocolEventPublisher: ProtocolEventPublisher
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        orderId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) = coroutineScope<Unit> {
        val itemId = ItemId(contract, orderId)
        orderRepository
            .findActiveById(orderId)
            .awaitSingleOrNull()
            ?.let { order ->
                val orderUpdate = async {
                    val savedOrder = orderRepository.coSave(order.copy(fill = order.take?.value ?: BigDecimal.ZERO))
                    protocolEventPublisher.onUpdate(savedOrder)
                }

                val itemUpdate = async {
                    itemRepository.coFindById(itemId)?.let { item ->
                        itemRepository.coSave(item.copy(owner = order.taker))
                    }?.let { item ->
                        protocolEventPublisher.onItemUpdate(item)
                    }

                }

                orderUpdate.await()
                itemUpdate.await()
            }
    }


    companion object {
        const val ID = "RegularSaleOrder.OrderClosed"
    }
}
