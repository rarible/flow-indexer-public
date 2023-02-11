package com.rarible.flow.scanner.migrations

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.Item
import com.rarible.flow.scanner.config.FlowApiProperties
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateMulti

//@ChangeUnit(
//    id = "ChangeLog00044KicksRoyalties",
//    order = "00044",
//    author = "flow"
//)
//class ChangeLog00044KicksRoyalties(
//    private val config: FlowApiProperties,
//    private val mongo: MongoTemplate
//) {
//
//    @Execution
//    fun changeSet() {
//        if (config.chainId == FlowChainId.MAINNET) {
//            mongo.updateMulti<Item>(
//                Query(
//                    Item::contract isEqualTo Contracts.KICKS.fqn(config.chainId)
//                ),
//                Update.update(Item::royalties.name, Contracts.KICKS.staticRoyalties(config.chainId))
//            )
//        }
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//
//    }
//}
