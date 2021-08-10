package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(OrderWithdrawn.ID)
class OrderWithdrawn(
    private val orderRepository: OrderRepository,
    private val itemService: ItemService,
    private val protocolEventPublisher: ProtocolEventPublisher,
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        orderId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) {
        orderRepository
            .coFindById(orderId)
            ?.let { order ->
                val cancelled = orderRepository.coSave(order.copy(canceled = true))
                itemService.unlist(order.itemId)
                protocolEventPublisher.onUpdate(cancelled)
            }
    }

    companion object {
        const val ID = "RegularSaleOrder.OrderWithdrawn"
    }
}
