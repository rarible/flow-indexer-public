package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.filters.ScrollingSort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface ItemCollectionRepository: ReactiveMongoRepository<ItemCollection, String>, ItemCollectionRepositoryCustom {

    fun findByCollectionOnChainId(collectionOnChainId: Long): Mono<ItemCollection>
}

interface ItemCollectionRepositoryCustom: ScrollingRepository<ItemCollection>

@Suppress("unused")
class ItemCollectionRepositoryCustomImpl(
    private val mongo: ReactiveMongoTemplate
): ItemCollectionRepositoryCustom {
    override fun defaultSort(): ScrollingSort<ItemCollection> {
        return CollectionFilter.Sort.LATEST_UPDATE
    }


    override fun findByQuery(query: Query): Flux<ItemCollection> {
        return mongo.find(query)
    }
}
