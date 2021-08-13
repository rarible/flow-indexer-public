package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.runBlocking
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
    ) = runBlocking<Unit> {
        val askType = fields["askType"] as String?
        val askId = (fields["askId"] as String).toLong()
        val bidType = fields["bidType"] as String
        val bidAmount = (fields["bidAmount"] as String).toBigDecimal()
        val buyerFee = (fields["buyerFee"] as String).toBigDecimal()
        val sellerFee = (fields["sellerFee"] as String).toBigDecimal()

        val itemId = ItemId(contract, askId)
        val item = itemRepository.coFindById(itemId)
        if(item?.owner != null) {
            val take = FlowAssetFungible(
                contract = EventId.of(bidType).contractAddress,
                value = bidAmount
            )
            val make = FlowAssetNFT(
                contract = item.contract, value = 1.toBigDecimal(), tokenId = item.tokenId
            )
            val order = orderRepository.coSave(
                Order(
                    id = orderId,
                    itemId = itemId,
                    maker = item.owner!!,
                    make = make,
                    take = take,
                    data = orderData(bidAmount, item),
                    amount = bidAmount,
                    buyerFee = buyerFee,
                    sellerFee = sellerFee,
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
                    date = LocalDateTime.now(ZoneOffset.UTC),
                    activity = FlowNftOrderActivityList(
                        price = bidAmount,
                        hash = UUID.randomUUID().toString(), //todo delete hash
                        maker = item.owner!!,
                        make = make,
                        take = take
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
        const val ID = "RegularSaleOrder.OrderOpened"
        val log by Log()
    }

}

