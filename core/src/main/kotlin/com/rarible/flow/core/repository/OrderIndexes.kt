package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Order
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index

object OrderIndexes {

    suspend fun createIndexes(mongo: ReactiveMongoTemplate) {
        ALL_INDEXES.forEach { mongo.indexOps(Order.COLLECTION).ensureIndex(it).awaitFirst() }
    }

    // TODO ChangeLog00017OrderIndexes, candidates to remove
    private val SELL_BY_ITEM: Index = Index()
        .on("make.contract", Sort.Direction.ASC)
        .on("make.tokenId", Sort.Direction.ASC)
        .named("make_contract_tokenId")

    private val BID_BY_ITEM: Index = Index()
        .on("take.contract", Sort.Direction.ASC)
        .on("take.tokenId", Sort.Direction.ASC)
        .named("take_contract_tokenId")

    // TODO ChangeLog00052OrderIndexByDbUpdatedAtAndIdField - Looks OK
    private val BY_DB_UPDATE_AT: Index = Index()
        .on(Order::dbUpdatedAt.name, Sort.Direction.ASC)
        .on(Order::id.name, Sort.Direction.ASC)

    private val ALL_INDEXES = listOf(
        SELL_BY_ITEM
    )

}