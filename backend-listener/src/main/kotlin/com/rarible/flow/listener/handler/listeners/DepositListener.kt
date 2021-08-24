package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.coroutineScope
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.*

@Component(DepositListener.ID)
class DepositListener(
    private val itemService: ItemService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: String,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ): Unit = coroutineScope {
        val to = FlowAddress(fields["to"]!! as String)

        val itemId = ItemId(contract, tokenId)
        val oldItem = itemService.byId(itemId)
        itemService
            .transferNft(itemId, to)
            ?.let { (item, ownership) ->
                protocolEventPublisher.onItemUpdate(item)
                protocolEventPublisher.onUpdate(ownership)

                itemHistoryRepository.coSave(
                    ItemHistory(
                        id = UUID.randomUUID().toString(),
                        date = Instant.now(Clock.systemUTC()),
                        activity = TransferActivity(
                            owner = to,
                            contract = contract,
                            tokenId = tokenId,
                            value = 1L,
                            transactionHash = blockInfo.transactionId,
                            blockHash = blockInfo.blockId,
                            blockNumber = blockInfo.blockHeight,
                            from = oldItem?.owner ?: FlowAddress("0x00"),
                            collection = item.collection
                        )
                    )
                )
            }
    }

    companion object {
        const val ID = "CommonNFT.Deposit"
    }
}
