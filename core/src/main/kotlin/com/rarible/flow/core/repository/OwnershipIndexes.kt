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

    // TODO taken from annotation, ensure it is really needed
    private val DATE_OWNER_CONTRACT_TOKEN_ID: Index = Index()
        .on(Ownership::date.name, Sort.Direction.DESC)
        .on(Ownership::owner.name, Sort.Direction.DESC)
        .on(Ownership::contract.name, Sort.Direction.DESC)
        .on(Ownership::tokenId.name, Sort.Direction.DESC)
        .named("date_owner_contract_tokenId")

    // TODO ChangeLog00006OwnershipIndex - check performance
    private val CONTRACT_TOKEN_ID: Index = Index()
        .on(Ownership::contract.name, Sort.Direction.ASC)
        .on(Ownership::tokenId.name, Sort.Direction.DESC)
        .named("contract_tokenId")

    private val ALL_INDEXES = listOf(
        DATE_OWNER_CONTRACT_TOKEN_ID,
        CONTRACT_TOKEN_ID
    )
}
