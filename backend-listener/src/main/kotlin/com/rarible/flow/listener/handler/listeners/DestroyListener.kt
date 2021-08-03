package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Component(DestroyListener.ID)
class DestroyListener(
    private val itemService: ItemService,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
): SmartContractEventHandler<Void> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ): Void = coroutineScope {

        val itemId = ItemId(contract, tokenId)

        itemHistoryRepository.coSave(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = LocalDateTime.now(ZoneOffset.UTC),
                activity = BurnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    transactionHash = blockInfo.transactionId,
                    blockHash = blockInfo.blockId,
                    blockNumber = blockInfo.blockHeight
                )
            )
        )

        val items = async {
            itemService.markDeleted(itemId)
        }
        val ownerships = async {
            ownershipRepository.deleteAllByContractAndTokenId(contract, tokenId).awaitSingle()
        }

        items.await().let {
            val result = protocolEventPublisher.onItemDelete(itemId)
            log.info("item delete message is sent: $result")
        }
        ownerships.await()

    }

    companion object {
        const val ID = "CommonNFT.Destroy"
        val log by Log()
    }
}
