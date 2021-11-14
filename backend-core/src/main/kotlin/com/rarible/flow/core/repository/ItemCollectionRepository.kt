package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemCollection
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface ItemCollectionRepository: ReactiveMongoRepository<ItemCollection, String>, ItemCollectionRepositoryCustom

interface ItemCollectionRepositoryCustom {
    fun search(filter: CollectionFilter, cont: String?, limit: Int?, sort: CollectionFilter.Sort): Flux<ItemCollection>
}

@Suppress("unused")
class ItemCollectionRepositoryCustomImpl(
    private val mongo: ReactiveMongoTemplate
): ItemCollectionRepositoryCustom {
    override fun search(filter: CollectionFilter, cont: String?, limit: Int?, sort: CollectionFilter.Sort): Flux<ItemCollection> {
        val query = sort.scroll(filter, cont, limit)
        return mongo.find(query)
    }
}