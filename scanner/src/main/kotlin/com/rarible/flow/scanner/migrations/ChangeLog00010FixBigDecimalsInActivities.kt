package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.Order
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

@ChangeUnit(
    id = "ChangeLog00010FixBigDecimalsInActivities",
    order = "00010",
    author = "flow"
)
class ChangeLog00010FixBigDecimalsInActivities(
    private val mongoTemplate: MongoTemplate
) {

    @Execution
    fun changeSet() {
        mongoTemplate.find<Order>(
            Query(
                ItemHistory::activity / BaseActivity::type isEqualTo FlowActivityType.SELL
            )
        ).forEach {
            mongoTemplate.save(it)
        }
    }

    @RollbackExecution
    fun rollBack() {

    }
}
