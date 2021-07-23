package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.*
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.coroutineScope
import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(OrderOpenedListener.ID)
class OrderOpenedListener(
    private val itemRepository: ItemRepository,
    private val orderRepository: OrderRepository,
    private val protocolEventPublisher: ProtocolEventPublisher
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(contract: FlowAddress, tokenId: TokenId, fields: Map<String, Any?>) {
        val askType = fields["askType"] as String
        val askId = (fields["askId"] as String).toLong()
        val bidType = fields["bidType"] as String
        val bidAmount = (fields["bidAmount"] as String).toBigDecimal()
        val buyerFee = (fields["buyerFee"] as String).toBigDecimal()
        val sellerFee = (fields["sellerFee"] as String).toBigDecimal()
        val maker = FlowAddress(fields["maker"] as String)

        val itemId = ItemId(contract, askId)
        orderRepository.save(
            Order(
                id = ObjectId.get(),
                itemId = itemId,
                maker = maker,
                amount = bidAmount,
            )
        )

        itemRepository
            .coFindById(itemId)
            ?.let {
                itemRepository.coSave(it.copy(listed = true))
            }
            ?.let { saved ->
                val result = protocolEventPublisher.onItemUpdate(saved)
                EventHandler.log.info("item update message is sent: $result")
            }
    }


    companion object {
        const val ID = "RegularSaleOrder.OrderOpened"
    }
}