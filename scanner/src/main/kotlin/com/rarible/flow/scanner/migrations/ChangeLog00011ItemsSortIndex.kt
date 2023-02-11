package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.Item
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps

//@ChangeUnit(
//    id = "ChangeLog00011ItemsSortIndex",
//    order = "00011",
//    author = "flow"
//)
//class ChangeLog00011ItemsSortIndex(
//    private val mongoTemplate: MongoTemplate
//) {
//    private val INDEX_NAME = "item_updatetAt_id_idx"
//
//    @Execution
//    fun changeSet() {
//        mongoTemplate
//            .indexOps<Item>()
//            .ensureIndex(
//                Index()
//                    .on(Item::updatedAt.name, Sort.Direction.DESC)
//                    .on("_id", Sort.Direction.DESC)
//                    .named(INDEX_NAME)
//            )
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//        mongoTemplate.indexOps<Item>().dropIndex(INDEX_NAME)
//    }
//}
