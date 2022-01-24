package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.ItemHistory
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@ChangeUnit(
    id = "ChangeLog00014RefactorItemHistory",
    order = "00013",
    author = "flow"
)
class ChangeLog00014RefactorItemHistory(
    private val mongo: ReactiveMongoTemplate
) {

    @Execution
    fun changeSet() {
        runBlocking {
            val query = Query().with(Sort.by(Sort.Direction.ASC, ItemHistory::date.name))
            mongo.find(query.addCriteria(Criteria()), ItemHistory::class.java).groupBy { it.log.transactionHash }
                .flatMap {
                    it.collectList()
                }
                .buffer()
                .asFlow()
        }
    }
}
