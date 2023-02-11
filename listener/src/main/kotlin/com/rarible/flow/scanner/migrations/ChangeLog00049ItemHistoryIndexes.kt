package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.ItemHistory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps

//@ChangeUnit(
//    id = "ChangeLog00049ItemHistoryIndex",
//    order = "00049",
//    author = "flow",
//)
//class ChangeLog00049ItemHistoryIndexes(
//    private val mongoTemplate: MongoTemplate,
//) {
//    @Execution
//    fun changeSet() {
//        indexes.map { (name, index) ->
//            mongoTemplate.indexOps<ItemHistory>().ensureIndex(index.named(name))
//        }
//    }
//
//    @RollbackExecution
//    fun rollback() {
//        indexes.keys.map {
//            mongoTemplate.indexOps<ItemHistory>().dropIndex(it)
//        }
//    }
//
//    companion object {
//        private val indexes = mapOf(
//            "tx_type_left_right" to Index()
//                .on("log.transactionHash", Sort.Direction.ASC)
//                .on("activity.type", Sort.Direction.ASC)
//                .on("activity.left.maker", Sort.Direction.ASC)
//                .on("activity.right.maker", Sort.Direction.ASC)
//                .background(),
//            "tx_type_from_to" to Index()
//                .on("log.transactionHash", Sort.Direction.ASC)
//                .on("activity.type", Sort.Direction.ASC)
//                .on("activity.from", Sort.Direction.ASC)
//                .on("activity.to", Sort.Direction.ASC)
//                .background(),
//        )
//    }
//}
