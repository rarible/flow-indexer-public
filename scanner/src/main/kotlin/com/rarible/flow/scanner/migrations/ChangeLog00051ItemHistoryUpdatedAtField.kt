package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.ItemHistory
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update


//TODO this will fail on production!!!
@ChangeUnit(
    id = "ChangeLog00051ItemHistoryUpdatedAtField",
    order = "00051",
    author = "flow",
)
class ChangeLog00051ItemHistoryUpdatedAtField(
    private val mongo: MongoTemplate
) {

    @Execution
    fun changeSet() {
        //mongo.updateMulti(Query(), AggregationUpdate.newUpdate().set("updatedAt").toValue("\$date"), ItemHistory::class.java)
        mongo.indexOps(ItemHistory::class.java).ensureIndex(Index().on("updatedAt", Sort.Direction.DESC).named("byUpdatedAt"))
    }

    @RollbackExecution
    fun rollback() {
        mongo.updateMulti(Query(), Update().unset("updatedAt"), ItemHistory::class.java)
        mongo.indexOps(ItemHistory::class.java).dropIndex("byUpdatedAt")
    }

}
