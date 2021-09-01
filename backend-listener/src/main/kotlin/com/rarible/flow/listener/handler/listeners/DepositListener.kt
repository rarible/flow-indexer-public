package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.*

@Component(DepositListener.ID)
class DepositListener(
    private val itemService: ItemService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler {

    override suspend fun handle(
        eventMessage: EventMessage
    ): Unit = coroutineScope {
        val event = Deposit(eventMessage.fields)

        val to = FlowAddress(event.to)
        val itemId = ItemId(eventMessage.eventId.nft(), event.id)
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
                            contract = item.contract,
                            tokenId = item.tokenId,
                            value = 1L,
                            transactionHash = eventMessage.blockInfo.transactionId,
                            blockHash = eventMessage.blockInfo.blockId,
                            blockNumber = eventMessage.blockInfo.blockHeight,
                            from = oldItem?.owner ?: FlowAddress("0x00"),
                            collection = item.collection
                        )
                    )
                )
            }
    }

    companion object {
        const val ID = "CommonNFT.Deposit"

        class Deposit(fields: Map<String, Any?>) {
            val id: Long by fields
            val to: String by fields
        }
    }
}
