package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.domain.ItemHistory
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.SetOperation
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where

@ChangeUnit(
    id = "ChangeLog00012ActivityDataMigration",
    order = "00012",
    author = "flow"
)
class ChangeLog00012ActivityDataMigration(
    private val mongoTemplate: ReactiveMongoTemplate,
) {

    @Execution
    fun changeSet() {
        runBlocking {
            mongoTemplate.findAll(ItemCollection::class.java).asFlow().collect { collection ->
                val query = Query().addCriteria(
                    where(ItemHistory::activity / BaseActivity::type).isEqualTo(FlowActivityType.MINT)
                        .and("activity.contract").isEqualTo(collection.id)
                )

                val creator = if (collection.name.endsWith("Rarible")) {
                    "\$activity.owner"
                } else {
                    collection.owner.formatted
                }
                val op = SetOperation("activity.creator", creator)
                val a = Aggregation.newUpdate(op)
                mongoTemplate.updateMulti(query, a, ItemHistory::class.java).then().awaitSingleOrNull()
            }
        }
    }

    @RollbackExecution
    fun rollBack() {

    }
}
