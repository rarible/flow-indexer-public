package com.rarible.flow.scanner.job

import com.rarible.flow.scanner.config.FlowListenerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

abstract class AbstractEntityCleanupJob<Entity, EntityId>(
    properties: FlowListenerProperties
) {
    protected val logger = LoggerFactory.getLogger(javaClass)
    private val cleanup = properties.cleanup

    protected abstract suspend fun find(fromId: EntityId?, batchSize: Int): Flow<Entity>

    protected abstract suspend fun cleanup(entity: Entity)

    protected abstract fun extractId(entity: Entity): EntityId

    protected abstract fun extractContract(entity: Entity): String?

    fun execute(fromId: EntityId?): Flow<EntityId> {
        return flow {
            var next = fromId
            do {
                next = cleanupFrom(next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    private suspend fun cleanupFrom(fromId: EntityId?): EntityId? {
        val batchSize = cleanup.batchSize
        val preservedCollections = cleanup.preservedCollections
        val batch = find(fromId, batchSize).toList()

        coroutineScope {
            batch.map {
                async {
                    if (extractContract(it) !in preservedCollections) cleanup(it)
                }
            }.awaitAll()
        }
        return batch.lastOrNull()?.let { extractId(it) }
    }
}