package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Item
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index

object ItemIndexes {

    suspend fun createIndexes(mongo: ReactiveMongoTemplate) {
        ALL_INDEXES.forEach { mongo.indexOps(Item.COLLECTION).ensureIndex(it).awaitFirst() }
    }

    // TODO ChangeLog00011ItemsSortIndex - check performance
    private val UPDATED_AT: Index = Index()
        .on(Item::updatedAt.name, Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .named("item_updatetAt_id_idx")

    // TODO ChangeLog00008AddItemsLastUpdateAndOwnerIndex - check performance
    private val OWNER_AND_UPDATED_AT: Index = Index()
        .on(Item::updatedAt.name, Sort.Direction.DESC)
        .on(Item::owner.name, Sort.Direction.DESC)
        .named("owner_and_updated_at_idx")

    private val ALL_INDEXES = listOf(
        UPDATED_AT,
        OWNER_AND_UPDATED_AT
    )
}
