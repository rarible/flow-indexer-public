package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.*

@Component(SaleOfferAvailable.ID)
class SaleOfferAvailable(
    private val itemRepository: ItemRepository,
    private val orderRepository: OrderRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler {

    override suspend fun handle(
        eventMessage: EventMessage
    ) = runBlocking<Unit> {
        val event = SaleOfferEvent(eventMessage.fields)


        val itemId = ItemId(event.nftType, event.nftId.toLong())
        val item = itemRepository.coFindById(itemId)
        if(item?.owner != null) {
            val price = event.price.toBigDecimal()
            val take = FlowAssetFungible(
                contract = event.ftVaultType,
                value = price
            )
            val make = FlowAssetNFT(
                contract = item.contract, value = 1.toBigDecimal(), tokenId = item.tokenId
            )
            val order = orderRepository.coSave(
                Order(
                    id = event.saleOfferResourceID.toLong(),
                    itemId = itemId,
                    maker = item.owner!!,
                    make = make,
                    take = take,
                    data = orderData(price, item),
                    amount = price,
                    collection = item.collection
                )
            )

            protocolEventPublisher.onUpdate(order)

            val saved = itemRepository.coSave(item.copy(listed = true))
            val result = protocolEventPublisher.onItemUpdate(saved)
            log.debug("Item update message is sent: $result")

            itemHistoryRepository.coSave(
                ItemHistory(
                    id = UUID.randomUUID().toString(),
                    date = Instant.now(Clock.systemUTC()),
                    activity = FlowNftOrderActivityList(
                        price = price,
                        hash = UUID.randomUUID().toString(), //todo delete hash
                        maker = item.owner!!,
                        make = make,
                        take = take,
                        collection = saved.collection,
                        tokenId = order.id
                    )
                )
            )
        } else {
            log.warn("Trying to sell deleted or non-existing item [{}]", itemId)
        }
    }

    fun orderData(fill: BigDecimal, item: Item): OrderData {
        val feesPaid = item.royalties.map { r ->
            Payout(r.address, fill * (r.fee / 100).toBigDecimal())
        }
        val sellerProfit = fill - feesPaid.foldRight(BigDecimal.ZERO) { payout, acc ->
            acc + payout.value
        }

        return OrderData(
            feesPaid + Payout(item.owner!!, sellerProfit),
            item.royalties.map { Payout(it.address, it.fee.toBigDecimal()) }
        )
    }


    companion object {
        const val ID = "NFTStorefront.SaleOfferAvailable"
        val log by Log()

        class SaleOfferEvent(fields: Map<String, Any?>) {
            val saleOfferResourceID: String by fields
            val nftType: String by fields
            val nftId: String by fields
            val ftVaultType: String by fields
            val price: String by fields
        }
    }

}
