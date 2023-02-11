package com.rarible.flow.scanner.migrations

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Part
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateMulti

//@ChangeUnit(
//    id = "ChangeLog00018OneFootballRoyalties",
//    order = "00018",
//    author = "flow"
//)
//class ChangeLog00018OneFootballRoyalties(
//    private val mongo: MongoTemplate
//) {
//
//    @Execution
//    fun changeSet() {
//        mongo.updateMulti<Item>(
//            Query(
//                Item::contract isEqualTo Contracts.ONE_FOOTBALL.fqn(FlowChainId.MAINNET)
//            ),
//            Update.update(Item::royalties.name, listOf(
//                Part(Contracts.ONE_FOOTBALL.deployments[FlowChainId.MAINNET]!!, 0.005)
//            ))
//        )
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//
//    }
//}
