package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowChainId
import com.rarible.core.task.Task
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.subscriber.flowDescriptorName
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findAllAndRemove
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Service

@Service
class CollectionService(
    private val mongo: ReactiveMongoTemplate
) {

    suspend fun purgeCollectionHistory(contract: Contracts, chainId: FlowChainId) {
        purgeCollectionEventLogs(contract)
        purgeItemHistory(contract, chainId)
        purgeDescriptor(contract)
    }

    suspend fun purgeCollectionEventLogs(contract: Contracts) {
        val query = Query(
            (FlowLogEvent::event / EventMessage::eventId / EventId::contractName).isEqualTo(contract.contractName)
        )
        val removed = mongo.remove<ItemHistory>(query).awaitFirstOrNull()
        if (removed == null || !removed.wasAcknowledged()) {
            logger.warn("Failed to delete flow_log_events for contract {}", contract)
        } else {
            logger.info("Deleted flow_log_events: {}", removed.deletedCount)
        }
    }

    suspend fun purgeItemHistory(contracts: Contracts, chainId: FlowChainId) {
        try {
            val items = mongo.findAllAndRemove<Item>(
                Query(Item::contract isEqualTo contracts.fqn(chainId))
            ).toIterable()
            mongo.remove<ItemMeta>(
                Query(
                    Criteria("_id").inValues(items.map { it.id }.toString())
                )
            ).awaitFirstOrNull()
        } catch (e: Throwable) {
            logger.warn("Skipping purgeItemHistory for {} at {}", contracts, chainId)
        }
    }

    suspend fun purgeDescriptor(contract: Contracts) {
        mongo.remove<Task>(
            Query(
                Task::param isEqualTo contract.flowDescriptorName()
            )
        ).awaitFirstOrNull()
    }

    companion object {
        val logger by Log()
    }
}