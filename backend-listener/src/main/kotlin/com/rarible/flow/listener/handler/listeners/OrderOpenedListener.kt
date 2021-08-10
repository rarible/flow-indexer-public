package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Component(OrderOpenedListener.ID)
class OrderOpenedListener(
    private val itemRepository: ItemRepository,
    private val orderRepository: OrderRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        orderId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) {
        val askType = fields["askType"] as String?
        val askId = (fields["askId"] as String).toLong()
        val bidType = fields["bidType"] as String
        val bidAmount = (fields["bidAmount"] as String).toBigDecimal()
        val buyerFee = (fields["buyerFee"] as String).toBigDecimal()
        val sellerFee = (fields["sellerFee"] as String).toBigDecimal()

        val itemId = ItemId(contract, askId)
        val item = itemRepository.coFindById(itemId)
        if(item?.owner != null) {
            val order = orderRepository.coSave(
                Order(
                    id = orderId,
                    itemId = itemId,
                    maker = item.owner!!,
                    make = FlowAssetNFT(contract = item.contract, value = 1.toBigDecimal(), tokenId = item.tokenId),
                    data = OrderData(listOf(), listOf()), //TODO calculate all payouts and fees
                    amount = bidAmount,
                    buyerFee = buyerFee,
                    sellerFee = sellerFee,
                    collection = item.collection
                )
            )

            protocolEventPublisher.onUpdate(order)

            itemRepository
                .coFindById(itemId)
                ?.let {
                    itemRepository.coSave(it.copy(listed = true))
                }
                ?.let { saved ->
                    val result = protocolEventPublisher.onItemUpdate(saved)
                    log.info("item update message is sent: $result")
                }
            itemHistoryRepository.coSave(
                ItemHistory(
                    id = UUID.randomUUID().toString(),
                    date = LocalDateTime.now(ZoneOffset.UTC),
                    activity = FlowNftOrderActivityList(
                        price = bidAmount,
                        hash = UUID.randomUUID().toString(), //todo delete hash
                        maker = item.owner!!,
                        make = FlowAssetNFT(
                            contract = contract,
                            value = BigDecimal.valueOf(1L),
                            tokenId = orderId
                        ),
                        take = FlowAssetFungible(
                            contract = EventId.of(bidType).contractAddress,
                            value = bidAmount
                        )
                    )
                )
            )
        } else {
            log.warn("Trying to sell deleted or non-existing item [{}]", itemId)
        }
    }


    companion object {
        const val ID = "RegularSaleOrder.OrderOpened"
        val log by Log()
    }

}

