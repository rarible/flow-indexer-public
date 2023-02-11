package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.ItemHistory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps

//@ChangeUnit(
//    id = "ChangeLog00006AddItemHistorySortIndex",
//    order = "00006",
//    author = "flow"
//)
//class ChangeLog00006AddItemHistorySortIndex(
//    private val mongoTemplate: MongoTemplate
//) {
//
//    @Execution
//    fun changeSet() {
//        mongoTemplate.indexOps<ItemHistory>().ensureIndex(
//        Index()
//            .on("date", Sort.Direction.DESC)
//            .on("log.transactionHash", Sort.Direction.DESC)
//            .on("log.eventIndex", Sort.Direction.DESC)
//            .named("default_order_idx")
//        )
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//        mongoTemplate.indexOps<ItemHistory>().dropIndex("default_order_idx")
//    }
//}
