package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.util.*

@Component(DestroyListener.ID)
class DestroyListener(
    private val itemService: ItemService,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
): SmartContractEventHandler {

    override suspend fun handle(
        eventMessage: EventMessage
    ): Unit = runBlocking {
        val event = CommonNFTDestroy(eventMessage.fields)
        val itemId = ItemId(eventMessage.eventId.collection(), event.id.toLong())
        val item = itemService.findAliveById(itemId)
        if(item != null) {
            log.info("Burning item [{}]...", itemId)
            saveHistory(item, eventMessage)
            itemService.markDeleted(itemId)
            protocolEventPublisher.onItemDelete(itemId)

            ownershipRepository
                .deleteAllByContractAndTokenId(item.contract, item.tokenId)
                .collectList()
                .awaitFirstOrDefault(emptyList())
                .forEach {
                    protocolEventPublisher.onDelete(it)
                }


            log.info("Item [{}] is burnt.", itemId)
        }

    }

    private suspend fun saveHistory(
        item: Item,
        eventMessage: EventMessage
    ) {
        val blockInfo = eventMessage.blockInfo
        itemHistoryRepository.coSave(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = eventMessage.timestamp.toInstant(ZoneOffset.UTC),
                activity = BurnActivity(
                    contract = item.contract,
                    tokenId = item.tokenId,
                    transactionHash = blockInfo.transactionId,
                    blockHash = blockInfo.blockId,
                    blockNumber = blockInfo.blockHeight,
                    collection = item.collection,
                    owner = item.owner!!.formatted
                )
            )
        )
    }

    companion object {
        const val ID = "CommonNFT.Destroy"
        val log by Log()

        class CommonNFTDestroy(fields: Map<String, Any?>) {
            val id: String by fields
        }
    }
}
