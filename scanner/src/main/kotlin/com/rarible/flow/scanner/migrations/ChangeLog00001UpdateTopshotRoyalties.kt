package com.rarible.flow.scanner.migrations

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Part
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update


@ChangeUnit(
    id = "ChangeLog00001UpdateTopshotRoyalties",
    order = "00001",
    author = "flow"
)
class ChangeLog00001UpdateTopshotRoyalties(
    private val mongoTemplate: MongoTemplate
) {

    @Execution
    fun changeSet() {
        val query = Query().addCriteria(Criteria.where("collection").regex("TopShot"))
        val royaltiesAddress = FlowAddress("0xbd69b6abdfcf4539")
        val update = Update().set("royalties", listOf(Part(royaltiesAddress, 0.05)))
        mongoTemplate.updateMulti(query, update, Item::class.java)
    }

    @RollbackExecution
    fun rollback() {
        val query = Query().addCriteria(Criteria.where("collection").regex("TopShot"))
        val update = Update().set("royalties", emptyList<Part>())
        mongoTemplate.updateMulti(query, update, Item::class.java)
    }
}
