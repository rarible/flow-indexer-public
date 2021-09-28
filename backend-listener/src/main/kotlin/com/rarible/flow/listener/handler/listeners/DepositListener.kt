package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAccessApi
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.simpleFlowScript
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.listener.service.ItemService
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.listener.handler.listeners.motogp.MotoGpMint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component
import java.io.File
import java.time.ZoneOffset
import java.util.*
import javax.annotation.Resource

@Component(DepositListener.ID)
class DepositListener(
    private val itemService: ItemService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository,
    private val orderRepository: OrderRepository,
    private val flowAccessApi: FlowAccessApi
) : SmartContractEventHandler {

    @Resource(name = "/script/borrow_card_metadata.cdc")
    lateinit var metadataScript: File

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

                val orderToComplete = orderRepository.findByItemId(item.id).awaitSingleOrNull()
                if(orderToComplete != null && orderToComplete.taker == null) {
                    val order = orderRepository.coSave(
                        orderToComplete.copy(taker = to)
                    )

                    itemHistoryRepository.coSave(
                        ItemHistory(
                            id = UUID.randomUUID().toString(),
                            date = eventMessage.timestamp.toInstant(ZoneOffset.UTC),
                            activity = FlowNftOrderActivitySell(
                                price = order.take.value,
                                left = OrderActivityMatchSide(
                                    order.maker.formatted, order.make
                                ),
                                right = OrderActivityMatchSide(
                                    order.taker!!.formatted, order.take
                                ),
                                blockHash = eventMessage.blockInfo.blockId,
                                blockNumber = eventMessage.blockInfo.blockHeight,
                                transactionHash = eventMessage.blockInfo.transactionId,
                                collection = item.collection,
                                tokenId = item.tokenId,
                                contract = item.contract
                            )
                        )
                    )
                } else if(item.creator == MotoGpMint.emptyAddress) {

                    val meta = flowAccessApi.simpleFlowScript {
                        script(metadataScript.readText())
                    }.jsonCadence

                    item.copy(creator = to)

                } else {
                    itemHistoryRepository.coSave(
                        ItemHistory(
                            id = UUID.randomUUID().toString(),
                            date = eventMessage.timestamp.toInstant(ZoneOffset.UTC),
                            activity = TransferActivity(
                                owner = to.formatted,
                                contract = item.contract,
                                tokenId = item.tokenId,
                                value = 1L,
                                transactionHash = eventMessage.blockInfo.transactionId,
                                blockHash = eventMessage.blockInfo.blockId,
                                blockNumber = eventMessage.blockInfo.blockHeight,
                                from = oldItem?.owner?.formatted ?: FlowAddress("0x00").formatted,
                                collection = item.collection
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
