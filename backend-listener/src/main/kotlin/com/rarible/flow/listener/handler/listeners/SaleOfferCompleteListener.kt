package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.*

@Component(SaleOfferCompleteListener.ID)
class SaleOfferCompleteListener(
    private val itemService: ItemService,
    private val orderRepository: OrderRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: String,
        orderId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) = runBlocking {
        val order = orderRepository
            .findActiveById(orderId)
            .awaitSingleOrNull()

        if (order != null) {
            if (fields["accepted"]!! as Boolean) {
                completeOffer(order, blockInfo)
            } else {
                cancelOffer(order)
            }
        } else {
            log.info("Order [{}] does not exist", orderId)
        }
    }

    private suspend fun completeOffer(
        order: Order?,
        blockInfo: BlockInfo
    ) {
        if (order != null) {
            val fill = order.take?.value ?: BigDecimal.ZERO
            val savedOrder = orderRepository.coSave(
                order.copy(
                    fill = fill
                )
            )
            protocolEventPublisher.onUpdate(savedOrder)

            val item = itemService.byId(order.itemId)
            if(item == null) {
                log.error("Trying to complete an order for non-existing item [{}]", order.itemId)
            } else {
                itemHistoryRepository.save(
                    ItemHistory(
                        id = UUID.randomUUID().toString(),
                        date = Instant.now(Clock.systemUTC()),
                        activity = FlowNftOrderActivitySell(
                            price = order.take?.value ?: BigDecimal.ZERO,
                            left = OrderActivityMatchSide(
                                order.maker, order.make
                            ),
                            right = OrderActivityMatchSide(
                                order.taker!!, order.take!!
                            ),
                            blockHash = blockInfo.blockId,
                            blockNumber = blockInfo.blockHeight,
                            transactionHash = blockInfo.transactionId,
                            collection = item.collection,
                            tokenId = savedOrder.id
                        )
                    )
                )
            }
        }
    }

    private suspend fun cancelOffer(
        order: Order
    ) {
        val cancelled = orderRepository.coSave(order.copy(canceled = true))
        itemService.unlist(order.itemId)
        protocolEventPublisher.onUpdate(cancelled)
        val item = itemService.byId(order.itemId)

        itemHistoryRepository.save(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()),
                activity = FlowNftOrderActivityCancelList(
                    price = order.amount,
                    hash = UUID.randomUUID().toString(), //todo delete hash
                    maker = item?.owner!!,
                    make = FlowAssetNFT(
                        contract = item.contract,
                        value = BigDecimal.valueOf(1L),
                        tokenId = order.id
                    ),
                    take = FlowAssetFungible(
                        contract = order.take?.contract.orEmpty(), //todo take can be empty?
                        value = order.amount
                    ),
                    collection = item.collection,
                    tokenId = order.id
                )
            )
        )
    }


    companion object {
        const val ID = "NFTStorefront.SaleOfferCompleted"
        private val log by Log()
    }
}
