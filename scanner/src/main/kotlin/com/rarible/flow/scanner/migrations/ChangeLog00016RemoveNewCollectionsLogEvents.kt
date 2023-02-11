package com.rarible.flow.scanner.migrations

import com.nftco.flow.sdk.FlowChainId
import com.rarible.core.common.mapAsync
import com.rarible.core.task.Task
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findAllAndRemove
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove

//@ChangeUnit(
//    id = "ChangeLog00016RemoveNewCollectionsLogEvents",
//    order = "00016",
//    author = "flow"
//)
//class ChangeLog00016RemoveNewCollectionsLogEvents(
//    private val mongo: ReactiveMongoOperations,
//) {
//
//    val logger by Log()
//
//    private val contracts = listOf(
//        Contracts.MATRIX_WORLD_FLOW_FEST, Contracts.MATRIX_WORLD_VOUCHER,
//        Contracts.ONE_FOOTBALL
//    )
//
//    @Execution
//    fun changeSet() {
//        runBlocking {
//            contracts.mapAsync {
//                val query = Query(
//                    (FlowLogEvent::event / EventMessage::eventId / EventId::contractName).isEqualTo(it.contractName)
//                )
//                val removed = mongo.remove<ItemHistory>(query).awaitFirstOrNull()
//                if (removed == null || !removed.wasAcknowledged()) {
//                    logger.warn("Failed to delete flow_log_events for contract {}", it)
//                } else {
//                    logger.info("Deleted flow_log_events: {}", removed.deletedCount)
//                }
//            }
//
//            contracts.mapAsync {
//                val items = mongo.findAllAndRemove<Item>(
//                    Query(Item::contract isEqualTo it.fqn(FlowChainId.MAINNET))
//                ).toIterable()
//                mongo.remove<ItemMeta>(
//                    Query(
//                        Criteria("_id").inValues(items.map { it.id }.toString())
//                    )
//                ).awaitFirstOrNull()
//            }
//
//            val removedTasks = mongo.remove<Task>(
//                Query(
//                    Task::param inValues listOf(
//                        "MatrixWorldFlowFestNFTDescriptor",
//                        "OneFootballCollectibleDescriptor",
//                        "MatrixWorldVoucherDescriptor"
//                    )
//                )
//            ).awaitFirstOrNull()
//
//            if (removedTasks == null || !removedTasks.wasAcknowledged()) {
//                logger.warn("Failed to delete tasks")
//            } else {
//                logger.info("Deleted tasks: {}", removedTasks.deletedCount)
//            }
//        }
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//    }
//}
