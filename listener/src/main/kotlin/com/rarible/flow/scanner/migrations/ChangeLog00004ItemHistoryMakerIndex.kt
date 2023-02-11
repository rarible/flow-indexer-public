package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.ItemHistory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

//@ChangeUnit(
//    id = "ChangeLog00004ItemHistoryMakerIndex",
//    order = "00004",
//    author = "flow"
//)
//class ChangeLog00004ItemHistoryMakerIndex(
//    private val mongoTemplate: MongoTemplate,
//) {
//
//    @Execution
//    fun changeSet() {
//        mongoTemplate.indexOps(ItemHistory::class.java)
//            .ensureIndex(Index().on("activity.maker", Sort.Direction.ASC).named("activity_maker"))
//    }
//
//    @RollbackExecution
//    fun rollback() {
//        mongoTemplate.indexOps(ItemHistory::class.java).dropIndex("activity_maker")
//    }
//}
