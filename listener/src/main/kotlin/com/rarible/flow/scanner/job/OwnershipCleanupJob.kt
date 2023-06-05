package com.rarible.flow.scanner.job

import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.util.offchainEventMarks
import com.rarible.flow.scanner.config.FlowListenerProperties
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
class OwnershipCleanupJob(
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    properties: FlowListenerProperties
) : AbstractEntityCleanupJob<Ownership, OwnershipId>(properties) {

    override suspend fun cleanup(entity: Ownership) {
        protocolEventPublisher.onDelete(
            ownership = entity,
            marks = offchainEventMarks()
        )
        ownershipRepository.delete(entity).awaitFirstOrNull()
        logger.info("Removed ownership {}", entity.id)
    }

    override suspend fun find(
        fromId: OwnershipId?,
        batchSize: Int
    ) = ownershipRepository.find(fromId, batchSize)

    override fun extractId(entity: Ownership) = entity.id
}
