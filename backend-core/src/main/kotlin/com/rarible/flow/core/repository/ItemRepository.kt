package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.filters.ScrollingSort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface ItemRepository : ReactiveMongoRepository<Item, ItemId>, ItemRepositoryCustom {
    fun findAllByCreator(creator: FlowAddress): Flux<Item>
    fun findAllByIdIn(ids: Set<ItemId>): Flux<Item>
}

interface ItemRepositoryCustom: ScrollingRepository<Item>

@Suppress("unused")
class ItemRepositoryCustomImpl(
    private val mongo: ReactiveMongoTemplate
) : ItemRepositoryCustom {

    override fun defaultSort(): ScrollingSort<Item> {
        return ItemFilter.Sort.LAST_UPDATE
    }

    override fun findByQuery(query: Query): Flux<Item> {
        return mongo.find(query)
    }
}
