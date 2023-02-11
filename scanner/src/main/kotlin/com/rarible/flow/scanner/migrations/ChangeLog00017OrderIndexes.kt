package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.Order
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps

//@ChangeUnit(
//    id = "ChangeLog00017OrderIndexes",
//    order = "00017",
//    author = "flow"
//)
//class ChangeLog00017OrderIndexes(
//    private val mongo: MongoTemplate
//) {
//
//    @Execution
//    fun changeSet() {
//        mongo.indexOps<Order>().ensureIndex(Index().on("make.contract", Sort.Direction.ASC).on("make.tokenId", Sort.Direction.ASC).named("make_contract_tokenId"))
//        mongo.indexOps<Order>().ensureIndex(Index().on("make.tokenId", Sort.Direction.ASC).named("make_tokenId"))
//
//        mongo.indexOps<Order>().ensureIndex(Index().on("take.contract", Sort.Direction.ASC).on("take.tokenId", Sort.Direction.ASC).named("take_contract_tokenId"))
//        mongo.indexOps<Order>().ensureIndex(Index().on("take.tokenId", Sort.Direction.ASC).named("take_tokenId"))
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//        mongo.indexOps<Order>().dropIndex("make_contract_tokenId")
//        mongo.indexOps<Order>().dropIndex("make_tokenId")
//
//        mongo.indexOps<Order>().dropIndex("take_contract_tokenId")
//        mongo.indexOps<Order>().dropIndex("take_tokenId")
//    }
//}
