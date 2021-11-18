package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.Item
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps

@ChangeUnit(
    id = "ChangeLog00007AddItemsLastUpdateIndex",
    order = "00007",
    author = "flow"
)
class ChangeLog00007AddItemsLastUpdateIndex(
    private val mongoTemplate: MongoTemplate
) {

    @Execution
    fun changeSet() {
        mongoTemplate.indexOps<Item>().ensureIndex(
        Index()
            .on(Item::updatedAt.name, Sort.Direction.DESC)
            .named(INDEX)
        )
    }

    @RollbackExecution
    fun rollBack() {
        mongoTemplate.indexOps<Item>().dropIndex(INDEX)
    }

    companion object {
        const val INDEX = "updated_at_idx"
    }
}
