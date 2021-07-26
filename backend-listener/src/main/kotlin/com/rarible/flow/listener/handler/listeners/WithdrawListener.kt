package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.events.BlockInfo
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(WithdrawListener.ID)
class WithdrawListener : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ): Unit {
        //todo need to remove ownership?
    }

    companion object {
        const val ID = "NFTProvider.Withdraw"
    }
}
