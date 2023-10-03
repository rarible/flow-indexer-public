package com.rarible.flow.scanner.migrations

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.Ownership
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "99998")
class ChangeLog99998DropIndexes {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val unusedIndices = mapOf(
        Item.COLLECTION to listOf(
            "owner_and_updated_at_idx",
            "date_tokenId",
            "updated_at_idx",

            // useless indexes from @Indexed annotation
            "collection",
            "creator",
            "contract",
            "owner"
        ),
        Ownership.COLLECTION to listOf(
            "date_owner_contract_tokenId",
            "contract_tokenId",
            "date_tokenId"
        ),

        Order.COLLECTION to listOf(
            "make_contract_tokenId",
            "make_tokenId",
            "take_contract_tokenId",
            "take_tokenId",

            "earliest_first_sort",
            "latest_first_sort",

            "collection",
            "createdAt",
            "dbUpdatedAt",
            "itemId",
            "lastUpdatedAt",
            "make",
            "maker",
            "platform",
            "status",
            "take",
            "taker",
            "type",
        ),
        ItemHistory.COLLECTION to listOf(
            "byUpdatedAt"
        )
    )

    @ChangeSet(
        id = "ChangeLog99998DropIndexes.dropIndexes",
        order = "99998",
        author = "protocol",
        runAlways = true
    )
    fun dropIndexes(
        @NonLockGuarded mongo: ReactiveMongoTemplate,
    ) = runBlocking {
        logger.info("Dropping unused indices")
        unusedIndices.forEach { (collection, indices) ->
            indices.forEach { index ->
                val existing = mongo.indexOps(collection).indexInfo.map { it.name }.collectList().awaitFirst()
                if (existing.contains(index)) {
                    mongo.indexOps(collection).dropIndex(index).awaitFirstOrNull()
                }
            }
        }
        logger.info("All unused indices has been dropped")
    }
}
