package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.Ownership
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateMulti

//@ChangeUnit(
//    id = "ChangeLog00005UpdateOwnershipsCreator",
//    order = "00005",
//    author = "flow"
//)
//class ChangeLog00005UpdateOwnershipsCreator(
//    private val mongoTemplate: MongoTemplate,
//) {
//
//    @Execution
//    fun changeSet() {
//        updateByContract("A.0b2a3299cc857e29.TopShot", "0x0b2a3299cc857e29")
//        updateByContract("A.f4264ac8f3256818.Evolution", "0xf4264ac8f3256818")
//        updateByContract("A.a49cc0ee46c54bfb.MotoGPCard", "0xa49cc0ee46c54bfb")
//
//    }
//
//    private fun updateByContract(contract: String, creator: String) {
//        val result = mongoTemplate.updateMulti<Ownership>(
//            Query(
//                Ownership::contract isEqualTo contract
//            ),
//            Update().set(Ownership::creator.name, creator)
//        )
//        logger.info("Updated {} records by contract {}", result.modifiedCount, contract)
//    }
//
//    @RollbackExecution
//    fun rollback() {
//
//    }
//
//    companion object {
//        val logger by Log()
//    }
//}
