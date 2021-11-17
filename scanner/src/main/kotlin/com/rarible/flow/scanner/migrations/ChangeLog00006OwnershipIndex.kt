package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.Ownership
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps

@ChangeUnit(
    id = "ChangeLog00006OwnershipIndex",
    order = "00006",
    author = "flow"
)
class ChangeLog00006OwnershipIndex(
    private val mongoTemplate: MongoTemplate
) {

    @Execution
    fun changeLog() {
        mongoTemplate.indexOps<Ownership>().ensureIndex(Index().named("contract_tokenId").on("contract", Sort.Direction.ASC).on("tokenId", Sort.Direction.ASC))
    }

    @RollbackExecution
    fun rollback() {
        mongoTemplate.indexOps<Ownership>().dropIndex("contract_tokenId")
    }
}
