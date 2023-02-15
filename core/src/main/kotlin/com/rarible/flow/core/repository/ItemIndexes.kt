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

    private val UPDATED_AT: Index = Index()
        .on(Item::updatedAt.name, Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .named("item_updatetAt_id_idx")
        .background()

    // TODO DELETED flag is not considered here!
    private val BY_OWNER_UPDATED_AT: Index = Index()
        .on(Item::owner.name, Sort.Direction.DESC)
        .on(Item::updatedAt.name, Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .background()

    private val BY_CREATOR_UPDATED_AT: Index = Index()
        .on(Item::creator.name, Sort.Direction.DESC)
        .on(Item::updatedAt.name, Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .background()

    private val BY_COLLECTION_UPDATED_AT: Index = Index()
        .on(Item::collection.name, Sort.Direction.DESC)
        .on(Item::updatedAt.name, Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .background()

    private val ALL_INDEXES = listOf(
        UPDATED_AT,
        BY_OWNER_UPDATED_AT,
        BY_CREATOR_UPDATED_AT,
        BY_COLLECTION_UPDATED_AT

    )
}
