package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
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
): SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ): Unit = runBlocking {
        val itemId = ItemId(contract, tokenId)
        val item = itemService.findAliveById(itemId)
        if(item != null) {
            log.info("Burning item [{}]...", itemId)
            saveHistory(contract, tokenId, blockInfo, item)
            itemService.markDeleted(itemId)
            protocolEventPublisher.onItemDelete(itemId)

            ownershipRepository
                .deleteAllByContractAndTokenId(contract, tokenId)
                .collectList()
                .awaitFirstOrDefault(emptyList())
                .forEach {
                    protocolEventPublisher.onDelete(it)
                }


            log.info("Item [{}] is burnt.", itemId)
        }

    }

    private suspend fun saveHistory(
        contract: FlowAddress,
        tokenId: TokenId,
        blockInfo: BlockInfo,
        item: Item
    ) {
        itemHistoryRepository.coSave(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = LocalDateTime.now(ZoneOffset.UTC),
                activity = BurnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    transactionHash = blockInfo.transactionId,
                    blockHash = blockInfo.blockId,
                    blockNumber = blockInfo.blockHeight,
                    collection = item.collection
                )
            )
        )
    }

    companion object {
        const val ID = "CommonNFT.Destroy"
        val log by Log()
    }
}
