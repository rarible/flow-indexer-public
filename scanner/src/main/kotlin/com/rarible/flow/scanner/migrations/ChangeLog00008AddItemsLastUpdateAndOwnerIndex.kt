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
    id = "ChangeLog00008AddItemsLastUpdateAndOwnerIndex",
    order = "00008",
    author = "flow"
)
class ChangeLog00008AddItemsLastUpdateAndOwnerIndex(
    private val mongoTemplate: MongoTemplate
) {

    @Execution
    fun changeSet() {
        mongoTemplate.indexOps<Item>().ensureIndex(
        Index()
            .on(Item::updatedAt.name, Sort.Direction.DESC)
            .on(Item::owner.name, Sort.Direction.DESC)
            .named(INDEX)
        )
    }

    @RollbackExecution
    fun rollBack() {
        mongoTemplate.indexOps<Item>().dropIndex(INDEX)
    }

    companion object {
        const val INDEX = "owner_and_updated_at_idx"
    }
}
