package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.service.ItemService
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Component(OrderWithdrawn.ID)
class OrderWithdrawn(
    private val orderRepository: OrderRepository,
    private val itemService: ItemService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val itemHistoryRepository: ItemHistoryRepository
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        orderId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) = runBlocking {
        val order = orderRepository.coFindById(orderId)
        if(order != null) {
            val cancelled = orderRepository.coSave(order.copy(canceled = true))
            itemService.unlist(order.itemId)
            protocolEventPublisher.onUpdate(cancelled)
            val item = itemService.byId(order.itemId)

            itemHistoryRepository.save(
                ItemHistory(
                    id = UUID.randomUUID().toString(),
                    date = LocalDateTime.now(ZoneOffset.UTC),
                    activity = FlowNftOrderActivityCancelList(
                        price = order.amount,
                        hash = UUID.randomUUID().toString(), //todo delete hash
                        maker = item?.owner!!,
                        make = FlowAssetNFT(
                            contract = contract,
                            value = BigDecimal.valueOf(1L),
                            tokenId = orderId
                        ),
                        take = FlowAssetFungible(
                            contract = order.take?.contract ?: FlowAddress("0x00"),
                            value = order.amount
                        ),
                        collection = item.collection
                    )
                )
            )
        }
    }

    companion object {
        const val ID = "RegularSaleOrder.OrderWithdrawn"
    }
}
