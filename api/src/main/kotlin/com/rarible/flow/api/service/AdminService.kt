package com.rarible.flow.api.service

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.util.Log
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderRepository: OrderRepository,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
) {

    val logger by Log()

    suspend fun deleteItemById(itemId: ItemId) {

        val item = itemRepository.findById(itemId).awaitFirstOrNull()
        if (item == null) {
            logger.warn("Item $itemId not found")
            return
        }

        itemRepository.delete(item).awaitFirstOrNull()
        orderRepository.deleteByItemId(itemId).asFlow().toList()
        val ownerships = ownershipRepository.deleteAllByContractAndTokenId(item.contract, item.tokenId)
            .asFlow().toList()
        itemHistoryRepository.deleteByItemId(itemId.contract, itemId.tokenId).asFlow().toList()

        protocolEventPublisher.onItemDelete(item.id)
        protocolEventPublisher.onDelete(ownerships)
    }
}