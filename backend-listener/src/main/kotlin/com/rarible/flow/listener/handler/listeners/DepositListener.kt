package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.*

@Component(DepositListener.ID)
class DepositListener(
    private val itemService: ItemService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository,
    private val orderRepository: OrderRepository
) : SmartContractEventHandler {

    override suspend fun handle(
        eventMessage: EventMessage
    ): Unit = coroutineScope {
        val event = Deposit(eventMessage.fields)

        val to = FlowAddress(event.to)
        val itemId = ItemId(eventMessage.eventId.collection(), event.id.toLong())
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

                val orderToComplete = orderRepository.findByItemId(item.id).awaitSingleOrNull()
                if(orderToComplete != null && orderToComplete.take == null && orderToComplete.fill != BigDecimal.ZERO) {
                    orderRepository.coSave(
                        orderToComplete.copy(taker = to)
                    )

                    itemHistoryRepository.save(
                        ItemHistory(
                            id = UUID.randomUUID().toString(),
                            date = Instant.now(Clock.systemUTC()),
                            activity = FlowNftOrderActivitySell(
                                price = orderToComplete.take?.value ?: BigDecimal.ZERO,
                                left = OrderActivityMatchSide(
                                    orderToComplete.maker, orderToComplete.make
                                ),
                                right = OrderActivityMatchSide(
                                    orderToComplete.taker!!, orderToComplete.take!!
                                ),
                                blockHash = eventMessage.blockInfo.blockId,
                                blockNumber = eventMessage.blockInfo.blockHeight,
                                transactionHash = eventMessage.blockInfo.transactionId,
                                collection = item.collection,
                                tokenId = item.tokenId
                            )
                        )
                    )
                }
            }
    }

    companion object {
        const val ID = "CommonNFT.Deposit"

        class Deposit(fields: Map<String, Any?>) {
            val id: String by fields
            val to: String by fields
        }
    }
}
