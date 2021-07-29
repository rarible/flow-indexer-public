package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.EventHandler
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
    private val orderRepository: OrderRepositoryR,
    private val protocolEventPublisher: ProtocolEventPublisher,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) {
        val askType = fields["askType"] as String?
        val askId = (fields["askId"] as String).toLong()
        val bidType = fields["bidType"] as String
        val bidAmount = (fields["bidAmount"] as String).toBigDecimal()
        val buyerFee = (fields["buyerFee"] as String).toBigDecimal()
        val sellerFee = (fields["sellerFee"] as String).toBigDecimal()
        val maker = FlowAddress(fields["maker"] as String)

        val itemId = ItemId(contract, askId)
        val item = itemRepository.coFindById(itemId)!!
        orderRepository.coSave(
            Order(
                id = ObjectId.get(),
                itemId = itemId,
                maker = maker,
                make = FlowAssetNFT(contract = item.contract, value = 1.toBigDecimal(), tokenId = item.tokenId),
                data = OrderData(listOf(), listOf()), //TODO calculate all payouts and fees
                amount = bidAmount,
                buyerFee = buyerFee,
                sellerFee = sellerFee,
            )
        )

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
                    maker = maker,
                    make = FlowAssetNFT(
                        contract = contract,
                        value = BigDecimal.valueOf(1L),
                        tokenId = tokenId
                    ),
                    take = FlowAssetFungible(
                        contract = FlowAddress(bidType),
                        value = bidAmount
                    )
                )
            )
        )
    }


    companion object {
        const val ID = "RegularSaleOrder.OrderOpened"
        val log by Log()
    }

}

