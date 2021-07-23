package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.TokenId
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(OrderWithdrawn.ID)
class OrderWithdrawn : SmartContractEventHandler<Unit> {

    override suspend fun handle(contract: FlowAddress, tokenId: TokenId, fields: Map<String, Any?>) {
        //todo ?
    }

    companion object {
        const val ID = "RegularSaleOrder.OrderWithdrawn"
    }
}