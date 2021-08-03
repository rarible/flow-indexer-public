package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Component(TransferListener.ID)
class TransferListener(
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) {
        log.info("Handling [$ID] at [$contract.$tokenId] with fields [${fields}]")

        val from = FlowAddress(fields["from"]!! as String)
        val to = FlowAddress(fields["to"]!! as String)

        itemHistoryRepository.coSave(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = LocalDateTime.now(),
                activity = TransferActivity(
                    owner = to,
                    contract = contract,
                    tokenId = tokenId,
                    value = 1L,
                    transactionHash = blockInfo.transactionId,
                    blockHash = blockInfo.blockId,
                    blockNumber = blockInfo.blockHeight,
                    from = from
                )
            )
        )
    }

    companion object {
        const val ID = "CommonNFT.Transfer"

        val log by Log()
    }
}
