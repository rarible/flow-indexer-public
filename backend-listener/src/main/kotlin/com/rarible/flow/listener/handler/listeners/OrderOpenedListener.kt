package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepositoryR
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(OrderOpenedListener.ID)
class OrderOpenedListener(
    private val itemRepository: ItemRepository,
    private val orderRepository: OrderRepositoryR,
    private val protocolEventPublisher: ProtocolEventPublisher
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ): Unit {
        val askType = fields["askType"] as String
        val askId = (fields["askId"] as String).toLong()
        val bidType = fields["bidType"] as String
        val bidAmount = (fields["bidAmount"] as String).toBigDecimal()
        val buyerFee = (fields["buyerFee"] as String).toBigDecimal()
        val sellerFee = (fields["sellerFee"] as String).toBigDecimal()
        val maker = FlowAddress(fields["maker"] as String)

        val itemId = ItemId(contract, askId)
        orderRepository.coSave(
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
