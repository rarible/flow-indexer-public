package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowChainId
import com.rarible.core.task.Task
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.NFTActivity
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
        purgeItemHistory(contract, chainId)
        purgeLogEvents(contract, chainId)
        purgeDescriptor(contract)
    }

    suspend fun purgeItemHistory(contract: Contracts, chainId: FlowChainId) {
        val query = Query(
            Criteria(
                "${ItemHistory::activity.name}.${NFTActivity::contract.name}"
            ).isEqualTo(contract.fqn(chainId))
        )
        val removed = mongo.remove<ItemHistory>(query).awaitFirstOrNull()
        if (removed == null || !removed.wasAcknowledged()) {
            logger.warn("Failed to delete item_history for contract {}", contract)
        } else {
            logger.info("Deleted item_history: {}", removed.deletedCount)
        }
    }

    suspend fun purgeLogEvents(contracts: Contracts, chainId: FlowChainId) {
        try {
            mongo.remove<FlowLogEvent>(
                Query(
                    Criteria().andOperator(
                        FlowLogEvent::event / EventMessage::eventId / EventId::contractAddress isEqualTo contracts.deployments[chainId],
                        FlowLogEvent::event / EventMessage::eventId / EventId::contractName isEqualTo contracts.contractName
                    )
                )
            ).awaitFirstOrNull()
        } catch (e: Throwable) {
            logger.warn("Skipping purgeLogEvents for {} at {}", contracts, chainId, e)
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