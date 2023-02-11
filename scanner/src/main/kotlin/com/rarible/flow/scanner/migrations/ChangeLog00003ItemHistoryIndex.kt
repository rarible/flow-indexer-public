package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.ItemHistory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

//@ChangeUnit(
//    id = "ChangeLog00003ItemHistoryIndex",
//    order = "00003",
//    author = "flow"
//)
//class ChangeLog00003ItemHistoryIndex(
//    private val mongoTemplate: MongoTemplate
//) {
//
//    @Execution
//    fun changeSet() {
//        mongoTemplate.indexOps(ItemHistory::class.java).ensureIndex(Index().on("activity.from", Sort.Direction.ASC).named("activity_from"))
//        mongoTemplate.indexOps(ItemHistory::class.java).ensureIndex(Index().on("activity.to", Sort.Direction.ASC).named("activity_to"))
//        mongoTemplate.indexOps(ItemHistory::class.java).ensureIndex(Index().on("activity.timestamp", Sort.Direction.ASC).named("activity_timestamp"))
//        mongoTemplate.indexOps(ItemHistory::class.java).ensureIndex(Index().on("activity.owner", Sort.Direction.ASC).named("activity_owner"))
//
//    }
//
//    @RollbackExecution
//    fun rollback() {
//        mongoTemplate.indexOps(ItemHistory::class.java).dropIndex("activity_from")
//        mongoTemplate.indexOps(ItemHistory::class.java).dropIndex("activity_to")
//        mongoTemplate.indexOps(ItemHistory::class.java).dropIndex("activity_timestamp")
//        mongoTemplate.indexOps(ItemHistory::class.java).dropIndex("activity_owner")
//    }
//}
