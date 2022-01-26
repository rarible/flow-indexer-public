package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.ItemHistory
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.where

@ChangeUnit(
    id = "ChangeLog00014RemoveDepositWithdrawnItemHistory",
    order = "00014",
    author = "flow"
)
class ChangeLog00014RemoveDepositWithdrawnItemHistory(
    private val mongo: ReactiveMongoOperations,
) {

    @Execution
    fun changeSet() {
        val query = Query().addCriteria(where(ItemHistory::activity / BaseActivity::type).`in`("DEPOSIT", "WITHDRAWN"))
        mongo.remove(query, ItemHistory::class.java).then().block()
    }

    @RollbackExecution
    fun rollBack() {
    }
}
