package com.rarible.flow.scanner.job

import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.TaskItemHistoryRepository
import com.rarible.flow.core.util.taskEventMarks
import com.rarible.flow.scanner.config.FlowListenerProperties
import org.springframework.stereotype.Component

@Component
class ActivityCleanupJob(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: TaskItemHistoryRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    properties: FlowListenerProperties
) : AbstractEntityCleanupJob<ItemHistory, String>(properties) {

    override suspend fun cleanup(entity: ItemHistory) {
        protocolEventPublisher.activity(
            history = entity,
            reverted = true,
            eventTimeMarks = taskEventMarks()
        )
        itemHistoryRepository.delete(entity)
        logger.info("Removed activity {}", entity.id)
    }

    override suspend fun find(
        fromId: String?,
        batchSize: Int
    ) = itemHistoryRepository.find(fromId, batchSize)

    override fun extractId(entity: ItemHistory) = entity.id
}
