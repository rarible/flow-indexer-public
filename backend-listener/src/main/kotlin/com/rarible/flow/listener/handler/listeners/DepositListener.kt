package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.coroutineScope
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(DepositListener.ID)
class DepositListener(
    private val itemService: ItemService,
    private val protocolEventPublisher: ProtocolEventPublisher
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: String,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ): Unit = coroutineScope {
        val to = FlowAddress(fields["to"]!! as String)

        itemService
            .transferNft(ItemId(contract, tokenId), to)
            ?.let { (item, ownership) ->
                protocolEventPublisher.onItemUpdate(item)
                protocolEventPublisher.onUpdate(ownership)
            }
    }

    companion object {
        const val ID = "CommonNFT.Deposit"
    }
}
