package com.rarible.flow.scanner.migrations

import com.nftco.flow.sdk.FlowChainId
import com.rarible.core.common.mapAsync
import com.rarible.core.task.Task
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.NFTActivity
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
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
import kotlin.contracts.contract

@ChangeUnit(
    id = "ChangeLog00053RemoveItemPT784",
    order = "00053",
    author = "flow"
)
class ChangeLog00053RemoveItemPT784(
    private val mongo: ReactiveMongoOperations,
) {
    val contract = "A.0d77ec47bbad8ef6.MatrixWorldVoucher"
    val tokenId = 1246
    val itemId = "A.0d77ec47bbad8ef6.MatrixWorldVoucher:1246"

    val logger by Log()

    @Execution
    fun changeSet() = runBlocking<Unit> {

        var query = Query(Item::id.isEqualTo(itemId))
        var removed = mongo.remove<Item>(query).awaitFirstOrNull()
        if (removed == null || !removed.wasAcknowledged()) {
            logger.warn("Failed to delete flow item for itemId {}", itemId)
        } else {
            logger.info("Deleted items: {}", removed.deletedCount)
        }

        query = Query(ItemMeta::itemId.isEqualTo(itemId))
        removed = mongo.remove<ItemMeta>(query).awaitFirstOrNull()
        if (removed == null || !removed.wasAcknowledged()) {
            logger.warn("Failed to delete flow ItemMeta for itemId {}", itemId)
        } else {
            logger.info("Deleted ItemMeta: {}", removed.deletedCount)
        }

        query = Query(Order::itemId.isEqualTo(itemId))
        removed = mongo.remove<Order>(query).awaitFirstOrNull()
        if (removed == null || !removed.wasAcknowledged()) {
            logger.warn("Failed to delete flow Order for itemId {}", itemId)
        } else {
            logger.info("Deleted Order: {}", removed.deletedCount)
        }

        query = Query(
            Ownership::contract.isEqualTo(contract)
                .and(Ownership::tokenId.name).isEqualTo(tokenId)
        )
        removed = mongo.remove<Ownership>(query).awaitFirstOrNull()
        if (removed == null || !removed.wasAcknowledged()) {
            logger.warn("Failed to delete flow Ownership for itemId {}", itemId)
        } else {
            logger.info("Deleted Ownership: {}", removed.deletedCount)
        }

        query = Query()
        query.addCriteria(
            Criteria
                .where("activity.contract").isEqualTo(contract)
                .and("activity.tokenId").isEqualTo(tokenId)
        )

        removed = mongo.remove<ItemHistory>(query).awaitFirstOrNull()
        if (removed == null || !removed.wasAcknowledged()) {
            logger.warn("Failed to delete flow ItemHistory for itemId {}", itemId)
        } else {
            logger.info("Deleted ItemHistory: {}", removed.deletedCount)
        }
    }

    @RollbackExecution
    fun rollBack() {
    }
}