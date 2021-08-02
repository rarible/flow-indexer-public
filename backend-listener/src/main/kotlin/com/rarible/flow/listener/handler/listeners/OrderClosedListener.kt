package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.events.BlockInfo
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(OrderClosedListener.ID)
class OrderClosedListener(
    private val orderRepository: OrderRepository,
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ): Unit {
        orderRepository
            .findByItemId(ItemId(contract, tokenId))
            .awaitSingleOrNull()
            ?.let {order ->
                orderRepository.coSave(order.copy(fill = 1))
            }
    }


    companion object {
        const val ID = "RegularSaleOrder.OrderClosed"
    }
}
