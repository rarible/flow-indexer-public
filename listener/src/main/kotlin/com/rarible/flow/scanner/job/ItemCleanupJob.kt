package com.rarible.flow.scanner.job

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.util.offchainEventMarks
import com.rarible.flow.scanner.config.FlowListenerProperties
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
class ItemCleanupJob(
    private val itemRepository: ItemRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    properties: FlowListenerProperties
) : AbstractEntityCleanupJob<Item, ItemId>(properties) {

    override suspend fun cleanup(entity: Item) {
        protocolEventPublisher.onItemDelete(
            itemId = entity.id,
            marks = offchainEventMarks()
        )
        itemRepository.delete(entity).awaitFirstOrNull()
        logger.info("Removed item {}", entity.id)
    }

    override suspend fun find(
        fromId: ItemId?,
        batchSize: Int
    ) = itemRepository.find(fromId, batchSize)

    override fun extractId(entity: Item) = entity.id
}