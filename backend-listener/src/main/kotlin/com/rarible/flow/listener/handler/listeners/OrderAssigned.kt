package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.OrderRepository
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(OrderAssigned.ID)
class OrderAssigned(
    private val orderRepository: OrderRepository,
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(contract: FlowAddress, tokenId: TokenId, fields: Map<String, Any?>) {
        val order = orderRepository.findByItemId(contract, tokenId)
        if(order != null) {
            orderRepository.save(order.copy(taker = FlowAddress(fields["to"]!! as String)))
        }
    }

    companion object {
        const val ID = "StoreShowCase.OrderAssigned"
    }
}