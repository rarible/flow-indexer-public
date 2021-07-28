package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.OrderRepositoryR
import com.rarible.flow.events.BlockInfo
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(OrderWithdrawn.ID)
class OrderWithdrawn(
    private val orderRepository: OrderRepositoryR
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ): Unit {
        orderRepository.deleteByItemId(ItemId(contract, tokenId)).awaitSingleOrNull()
    }

    companion object {
        const val ID = "RegularSaleOrder.OrderWithdrawn"
    }
}
