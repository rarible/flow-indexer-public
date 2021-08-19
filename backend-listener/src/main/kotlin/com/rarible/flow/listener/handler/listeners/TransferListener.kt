package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.log.Log
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.*

@Component(TransferListener.ID)
class TransferListener(
    private val itemHistoryRepository: ItemHistoryRepository,
    private val itemService: ItemService
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: String,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) {
        log.info("Handling [$ID] at [$contract.$tokenId] with fields [${fields}]")

        val from = FlowAddress(fields["from"]!! as String)
        val to = FlowAddress(fields["to"]!! as String)

        val item = itemService.byId(ItemId(contract, tokenId))
        // check if item exists and is not transferred yet
        if(item != null && item.owner != to) {
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
                        from = from,
                        collection = item.collection
                    )
                )
            )
        }
    }

    companion object {
        const val ID = "CommonNFT.Transfer"

        val log by Log()
    }
}
