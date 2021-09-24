package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.listener.service.ItemService
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.ZoneOffset
import java.util.*

@Component(SaleOfferCompleteListener.ID)
class SaleOfferCompleteListener(
    private val itemService: ItemService,
    private val orderRepository: OrderRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler {

    override suspend fun handle(
        eventMessage: EventMessage
    ) = runBlocking {
        val event = SaleOfferCompleted(eventMessage.fields)
        val order = orderRepository
            .findActiveById(event.saleOfferResourceID.toLong())
            .awaitSingleOrNull()

        if (order != null) {
            if (event.accepted.toBoolean()) {
                completeOffer(order)
            } else {
                cancelOffer(order, eventMessage)
            }
        } else {
            log.info("Order [{}] does not exist", event.saleOfferResourceID)
        }
    }

    private suspend fun completeOffer(
        order: Order,
    ) {
        val fill = order.take.value
        val savedOrder = orderRepository.coSave(
            order.copy(
                fill = fill
            )
        )
        protocolEventPublisher.onUpdate(savedOrder)
    }

    private suspend fun cancelOffer(
        order: Order,
        eventMessage: EventMessage
    ) {
        val cancelled = orderRepository.coSave(order.copy(cancelled = true))
        itemService.unlist(order.itemId)
        protocolEventPublisher.onUpdate(cancelled)
        val item = itemService.byId(order.itemId)

        itemHistoryRepository.save(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = eventMessage.timestamp.toInstant(ZoneOffset.UTC),
                activity = FlowNftOrderActivityCancelList(
                    price = order.amount,
                    hash = order.id.toString(), //todo delete hash
                    maker = item?.owner!!.formatted,
                    make = FlowAssetNFT(
                        contract = item.contract,
                        value = BigDecimal.valueOf(1L),
                        tokenId = item.tokenId
                    ),
                    take = FlowAssetFungible(
                        contract = order.take.contract,
                        value = order.amount
                    ),
                    collection = item.collection,
                    tokenId = item.tokenId,
                    contract = item.contract
                )
            )
        )
    }


    companion object {
        const val ID = "NFTStorefront.SaleOfferCompleted"
        private val log by Log()

        class SaleOfferCompleted(fields: Map<String, Any?>) {
            val saleOfferResourceID: String by fields
            val storefrontResourceID: String by fields
            val accepted: String by fields
        }
    }
}
