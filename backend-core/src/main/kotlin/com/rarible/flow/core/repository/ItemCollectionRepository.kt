package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ItemCollectionRepository: ReactiveMongoRepository<ItemCollection, String>, ItemCollectionRepositoryCustom

interface ItemCollectionRepositoryCustom: ContinuationRepositoryCustom<ItemCollection, CollectionFilter>

@Suppress("unused")
class ItemCollectionRepositoryCustomImpl(
    private val mongo: ReactiveMongoTemplate
): ItemCollectionRepositoryCustom {
    override fun search(filter: CollectionFilter, cont: Continuation?, limit: Int?): Flow<ItemCollection> {
        val criteria = filter.criteria()
        val query = Query.query(criteria).with(
            Sort.by(
                Sort.Order.desc(ItemCollection::createdDate.name),
                Sort.Order.desc(ItemCollection::id.name)
            )
        ).limit(limit ?: ItemRepositoryCustomImpl.DEFAULT_LIMIT)

        return mongo.find<ItemCollection>(query).asFlow()
    }
}