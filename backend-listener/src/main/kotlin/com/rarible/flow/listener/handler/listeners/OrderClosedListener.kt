package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.OrderRepository
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(OrderOpenedListener.ID)
class OrderClosedListener(
    private val orderRepository: OrderRepository,
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(contract: FlowAddress, tokenId: TokenId, fields: Map<String, Any?>) {
        val order = orderRepository.findByItemId(contract, tokenId)
        if(order != null) {
            orderRepository.save(order.copy(fill = 1))
        }
    }


    companion object {
        const val ID = "RegularSaleOrder.OrderClosed"
    }
}