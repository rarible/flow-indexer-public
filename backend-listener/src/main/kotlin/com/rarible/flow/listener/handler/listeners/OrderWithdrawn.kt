package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(OrderWithdrawn.ID)
class OrderWithdrawn(
    private val orderRepository: OrderRepository,
    private val itemRepository: ItemRepository,
    private val protocolEventPublisher: ProtocolEventPublisher
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) {
        val itemId = ItemId(contract, tokenId)
        itemRepository.unlist(itemId)
        orderRepository
            .deleteByItemId(itemId)
            .awaitSingleOrNull()
            ?.let { order ->
                protocolEventPublisher.onUpdate(order)
            }
    }

    companion object {
        const val ID = "RegularSaleOrder.OrderWithdrawn"
    }
}
