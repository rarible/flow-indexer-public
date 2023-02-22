package com.rarible.flow.scanner.job

import com.rarible.flow.scanner.config.FlowListenerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractEntityCleanupJob<Entity, EntityId>(
    private val properties: FlowListenerProperties
) {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

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
        val batchSize = properties.cleanup.batchSize
        val preservedCollections = properties.cleanup.preservedCollections
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