package com.rarible.flow.scanner.migrations

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.Item
import com.rarible.flow.scanner.config.FlowApiProperties
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateMulti

//@ChangeUnit(
//    id = "ChangeLog00020MatrixWorldFlowFestRoyalties",
//    order = "00020",
//    author = "flow"
//)
//class ChangeLog00020MatrixWorldFlowFestRoyalties(
//    private val config: FlowApiProperties,
//    private val mongo: MongoTemplate
//) {
//
//    @Execution
//    fun changeSet() {
//        mongo.updateMulti<Item>(
//            Query(
//                Item::contract isEqualTo Contracts.MATRIX_WORLD_FLOW_FEST.fqn(config.chainId)
//            ),
//            Update.update(Item::royalties.name, Contracts.MATRIX_WORLD_FLOW_FEST.staticRoyalties(config.chainId))
//        )
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//
//    }
//}
