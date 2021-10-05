package com.rarible.flow.listener.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class OrderService(
    val orderRepository: OrderRepository
) {

    suspend fun cancelOrderByItemIdAndMaker(itemId: ItemId, maker: FlowAddress): Order? {
        return orderRepository
            .findByItemIdAndCancelledAndMaker(itemId, false, maker)
            .awaitSingleOrNull()
            ?.let { order ->
                orderRepository.coSave(order.cancel())
            }
    }
}