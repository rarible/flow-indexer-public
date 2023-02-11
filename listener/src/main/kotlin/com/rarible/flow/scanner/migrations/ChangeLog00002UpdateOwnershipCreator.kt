package com.rarible.flow.scanner.migrations

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Ownership
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

//@ChangeUnit(
//    id = "ChangeLog00002UpdateOwnershipCreator",
//    order = "00002",
//    author = "flow"
//)
//class ChangeLog00002UpdateOwnershipCreator(
//    private val mongoTemplate: MongoTemplate,
//    private val appProperties: AppProperties
//) {
//
//    @Execution
//    fun changeSet() {
//        runBlocking {
//            updateTopshot()
//            updateEvolution()
//            updateMotoGP()
//        }
//    }
//
//    @RollbackExecution
//    fun rollback() {}
//
//    private fun updateMotoGP() {
//        val query = Query()
//        query.addCriteria(Criteria.where("contract").regex("MotoGPCard").and("creator").exists(false))
//        val update = Update()
//        val creator = when (appProperties.chainId) {
//            FlowChainId.TESTNET -> FlowAddress("0x01658d9b94068f3c")
//            FlowChainId.MAINNET -> FlowAddress("0xa49cc0ee46c54bfb")
//            else -> throw IllegalStateException("Unsupported chainId: ${appProperties.chainId}")
//        }
//        update.set("creator", creator)
//        mongoTemplate.updateMulti(query, update, Ownership::class.java)
//    }
//
//    private fun updateEvolution() {
//        val query = Query()
//        query.addCriteria(Criteria.where("contract").regex("Evolution").and("creator").exists(false))
//        val update = Update()
//        val creator = when (appProperties.chainId) {
//            FlowChainId.TESTNET -> FlowAddress("0x01658d9b94068f3c")
//            FlowChainId.MAINNET -> FlowAddress("0xf4264ac8f3256818")
//            else -> throw IllegalStateException("Unsupported chainId: ${appProperties.chainId}")
//        }
//        update.set("creator", creator)
//        mongoTemplate.updateMulti(query, update, Ownership::class.java)
//    }
//
//    private fun updateTopshot() {
//        val query = Query()
//        query.addCriteria(Criteria.where("contract").regex("TopShot").and("creator").exists(false))
//        val update = Update()
//        val creator = when (appProperties.chainId) {
//            FlowChainId.TESTNET -> FlowAddress("0x01658d9b94068f3c")
//            FlowChainId.MAINNET -> FlowAddress("0x0b2a3299cc857e29")
//            else -> throw IllegalStateException("Unsupported chainId: ${appProperties.chainId}")
//        }
//        update.set("creator", creator)
//        mongoTemplate.updateMulti(query, update, Ownership::class.java)
//    }
//}
