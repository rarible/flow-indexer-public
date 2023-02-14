package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Ownership
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index

object OwnershipIndexes {

    suspend fun createIndexes(mongo: ReactiveMongoTemplate) {
        ALL_INDEXES.forEach { mongo.indexOps(Ownership.COLLECTION).ensureIndex(it).awaitFirst() }
    }

    private val BY_UPDATED_AT: Index = Index()
        .on(Ownership::date.name, Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .background()

    private val BY_OWNER_UPDATED_AT: Index = Index()
        .on(Ownership::owner.name, Sort.Direction.DESC)
        .on(Ownership::date.name, Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .background()

    private val BY_CONTRACT_UPDATED_AT: Index = Index()
        .on(Ownership::contract.name, Sort.Direction.DESC)
        .on(Ownership::date.name, Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .background()

    private val BY_CONTRACT_TOKEN_ID_UPDATED_AT: Index = Index()
        .on(Ownership::contract.name, Sort.Direction.DESC)
        .on(Ownership::tokenId.name, Sort.Direction.DESC)
        .on(Ownership::date.name, Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .background()

    private val ALL_INDEXES = listOf(
        BY_UPDATED_AT,
        BY_OWNER_UPDATED_AT,
        BY_CONTRACT_UPDATED_AT,
        BY_CONTRACT_TOKEN_ID_UPDATED_AT
    )
}
