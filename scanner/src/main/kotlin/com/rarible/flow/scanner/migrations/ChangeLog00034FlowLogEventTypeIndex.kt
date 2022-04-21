package com.rarible.flow.scanner.migrations

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowLogEvent
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.domain.Sort
import org.springframework.data.mapping.div
import org.springframework.data.mapping.toDotPath
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps

@ChangeUnit(
    id = "ChangeLog00034FlowLogEventTypeIndex",
    order = "00034",
    author = "flow"
)
class ChangeLog00034FlowLogEventTypeIndex(
    private val mongoTemplate: MongoTemplate
) {
    private val INDEX = "flow_log_event_type"

    @Execution
    fun changeSet() {
        mongoTemplate
            .indexOps<FlowLogEvent>()
            .ensureIndex(
                Index()
                    .on((FlowLogEvent::log / FlowLog::eventType).toDotPath(), Sort.Direction.ASC)
                    .named(INDEX)
            )
    }

    @RollbackExecution
    fun rollBack() {
        mongoTemplate
            .indexOps<FlowLogEvent>()
            .dropIndex(INDEX)
    }
}
