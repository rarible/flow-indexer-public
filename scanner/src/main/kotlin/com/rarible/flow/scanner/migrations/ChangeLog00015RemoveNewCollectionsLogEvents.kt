package com.rarible.flow.scanner.migrations

import com.rarible.core.common.mapAsync
import com.rarible.core.task.Task
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove

@ChangeUnit(
    id = "ChangeLog00015RemoveNewCollectionsLogEvents",
    order = "00015",
    author = "flow"
)
class ChangeLog00015RemoveNewCollectionsLogEvents(
    private val mongo: ReactiveMongoOperations,
) {

    val logger by Log()
    
    private val contracts = listOf(
        Contracts.MATRIX_WORLD_FLOW_FEST, Contracts.MATRIX_WORLD_VOUCHER,
        Contracts.ONE_FOOTBALL, Contracts.STARLY_CARD
    ).map {
        it.contractName
    }

    @Execution
    fun changeSet() {
        runBlocking {
            contracts.mapAsync {
                val query = Query(
                    (FlowLogEvent::event / EventMessage::eventId / EventId::contractName).isEqualTo(it)
                )
                val removed = mongo.remove<ItemHistory>(query).awaitFirstOrNull()
                if(removed == null || !removed.wasAcknowledged()) {
                    logger.warn("Failed to delete flow_log_events for contract {}", it)
                } else {
                    logger.info("Deleted flow_log_events: {}", removed.deletedCount)
                }
            }

            val removedTasks = mongo.remove<Task>(
                Query(
                    Task::param inValues listOf(
                        "MatrixWorldSubscriberDescriptor",
                        "OneFootballCollectibleDescriptor",
                        "MatrixWorldVoucherDescriptor",
                        "StarlyCardDescriptor"
                    )
                )
            ).awaitFirstOrNull()

            if(removedTasks == null || !removedTasks.wasAcknowledged()) {
                logger.warn("Failed to delete tasks")
            } else {
                logger.info("Deleted tasks: {}", removedTasks.deletedCount)
            }
        }
    }

    @RollbackExecution
    fun rollBack() { }
}
