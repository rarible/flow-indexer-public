package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component(SaleOfferCut.ID)
class SaleOfferCut(
    private val itemRepository: ItemRepository,
    private val orderRepository: OrderRepository
) : SmartContractEventHandler {

    override suspend fun handle(
        eventMessage: EventMessage
    ) = runBlocking<Unit> {
        val event = SaleOfferCutEvent(eventMessage.fields)


        val orderId = event.saleOfferResourceID.toLong()
        val order = orderRepository.coFindById(orderId)
        if (order != null) {
            val item = itemRepository.coFindById(order.itemId)

            if (item != null) {
                val royalties = item.royalties
                val data = updateOrderData(royalties, order.data, event)
                orderRepository.coSave(order.copy(data = data))
            } else {
                log.warn("Order [{}] contains non-existing item [{}]", orderId, order.itemId)
            }
        } else {
            log.warn("Order [{}] was not found", orderId)
        }
    }


    companion object {
        const val ID = "NFTStorefront.SaleOfferCut"
        val log by Log()

        class SaleOfferCutEvent(fields: Map<String, Any?>) {
            val storefrontAddress: String by fields
            val saleOfferResourceID: String by fields
            val address: String by fields
            val amount: String by fields
        }

        fun updateOrderData(
            royalties: List<Part>,
            orderData: OrderData,
            event: SaleOfferCutEvent
        ): OrderData {
            val amount = event.amount.toBigDecimal()
            val address = FlowAddress(event.address)
            val isRoyalty = royalties.any { it.address == address}

            return OrderData(
                orderData.payouts + Payout(address, amount),
                if(isRoyalty) orderData.originalFees else orderData.originalFees +  Payout(address, amount)
            )
        }
    }

}
