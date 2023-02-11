package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.Order
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

//@ChangeUnit(
//    id = "ChangeLog00052OrderIndexByDbUpdatedAtAndIdField",
//    order = "00052",
//    author = "flow",
//)
//class ChangeLog00052OrderIndexByDbUpdatedAtAndIdField(
//    private val mongoTemplate: MongoTemplate
//) {
//
//    @Execution
//    fun changeSet() {
//        mongoTemplate.indexOps(Order::class.java).ensureIndex(
//            Index()
//                .on(Order::dbUpdatedAt.name,Sort.Direction.ASC)
//                .on(Order::id.name, Sort.Direction.ASC)
//        )
//    }
//
//    @RollbackExecution
//    fun rollBack(){}
//}
