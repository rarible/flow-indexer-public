package com.rarible.flow.scanner.migrations

import com.rarible.core.task.Task
import com.rarible.flow.core.domain.*
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

//@ChangeUnit(
//    id = "ChangeLog00029RemoveVersusData",
//    order = "00029",
//    author = "flow",
//)
//class ChangeLog00029RemoveVersusData(
//    private val mongo: ReactiveMongoOperations,
//) {
//
//    @Execution
//    fun changeSet() {
//        cleanCollectionData("Art", "A.d796ff17107bbff6.Art", "ArtDescriptor")
//    }
//
//    @Suppress("SameParameterValue")
//    private fun cleanCollectionData(
//        contractName: String,
//        collectionId: String,
//        collectionDescriptor: String,
//    ) = mapOf(
//        Task::class.java to Criteria("param").isEqualTo(collectionDescriptor),
//        FlowLogEvent::class.java to Criteria("event.eventId.contractName").isEqualTo(contractName),
//        ItemHistory::class.java to Criteria("activity.contract").isEqualTo(collectionId),
//        Item::class.java to Criteria("contract").isEqualTo(collectionId),
//        Ownership::class.java to Criteria("contract").isEqualTo(collectionId),
//        ItemMeta::class.java to Criteria("_id").regex("$collectionId:.*"),
//    ).forEach { (entityClass, criteria) ->
//        mongo.remove(Query(criteria), entityClass).block()
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//    }
//}
