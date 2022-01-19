package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor
import reactor.core.publisher.Flux

interface ItemRepository : ReactiveMongoRepository<Item, ItemId>, ItemRepositoryCustom {
    fun findAllByCreator(creator: FlowAddress): Flux<Item>
}

interface ItemRepositoryCustom {
    fun search(filter: ItemFilter, cont: String?, limit: Int?, sort: ItemFilter.Sort): Flux<Item>
}

@Suppress("unused")
class ItemRepositoryCustomImpl(
    private val mongo: ReactiveMongoTemplate
) : ItemRepositoryCustom {

    override fun search(filter: ItemFilter, cont: String?, limit: Int?, sort: ItemFilter.Sort): Flux<Item> {
        val query = sort.scroll(filter, cont, limit)
        return mongo.find(query)
    }

}
