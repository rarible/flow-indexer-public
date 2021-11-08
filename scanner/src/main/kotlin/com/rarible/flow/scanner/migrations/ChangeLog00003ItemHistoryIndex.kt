package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.ItemHistory
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index

@ChangeUnit(
    id = "ChangeLog00003ItemHistoryIndex",
    order = "00003",
    author = "flow"
)
class ChangeLog00003ItemHistoryIndex(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    @Execution
    fun changeSet() {
        mongoTemplate.indexOps(ItemHistory::class.java).ensureIndex(Index().on("activity.from", Sort.Direction.ASC).named("activity_from"))
            .and(mongoTemplate.indexOps(ItemHistory::class.java).ensureIndex(Index().on("activity.to", Sort.Direction.ASC).named("activity_to")))
            .and(mongoTemplate.indexOps(ItemHistory::class.java).ensureIndex(Index().on("activity.timestamp", Sort.Direction.ASC).named("activity_timestamp")))
            .and(mongoTemplate.indexOps(ItemHistory::class.java).ensureIndex(Index().on("activity.owner", Sort.Direction.ASC).named("activity_owner")))
            .subscribe()
    }

    @RollbackExecution
    fun rollback() {
        mongoTemplate.indexOps(ItemHistory::class.java).dropIndex("activity_from")
            .and(mongoTemplate.indexOps(ItemHistory::class.java).dropIndex("activity_to"))
            .and(mongoTemplate.indexOps(ItemHistory::class.java).dropIndex("activity_timestamp"))
            .and(mongoTemplate.indexOps(ItemHistory::class.java).dropIndex("activity_owner"))
            .subscribe()
    }
}
