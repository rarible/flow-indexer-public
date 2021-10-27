package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.log.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor
import reactor.core.publisher.Flux

interface ItemRepository : ReactiveMongoRepository<Item, ItemId>, ItemRepositoryCustom,
    ReactiveQuerydslPredicateExecutor<Item> {

    fun findAllByCreator(creator: FlowAddress): Flux<Item>

}

interface ItemRepositoryCustom {
    suspend fun search(filter: ItemFilter, cont: String?, limit: Int?): Flow<Item>
}

@Suppress("unused")
class ItemRepositoryCustomImpl(
    private val mongo: ReactiveMongoTemplate
) : ItemRepositoryCustom {

    override suspend fun search(filter: ItemFilter, cont: String?, limit: Int?): Flow<Item> {
        val criteria = filter.criteria().scrollTo(cont, filter.sort)
        val query = Query.query(criteria).with(
            mongoSort(filter.sort)
        ).limit(limit ?: DEFAULT_LIMIT)

        return mongo.find<Item>(query).collectList().awaitFirst().asFlow() //TODO
    }

    private fun mongoSort(sort: ItemFilter.Sort?): Sort {
        return when (sort) {
            ItemFilter.Sort.LAST_UPDATE -> Sort.by(
                Sort.Order.desc(Item::mintedAt.name),
                Sort.Order.desc(Item::id.name)
            )
            else -> Sort.unsorted()
        }
    }

    private fun Criteria.scrollTo(continuation: String?, sort: ItemFilter.Sort?): Criteria = when (sort) {
        ItemFilter.Sort.LAST_UPDATE -> Cont.scrollDesc(
            this,
            continuation,
            Item::mintedAt,
            Item::id
        )
        else -> this
    }

    companion object {
        const val DEFAULT_LIMIT: Int = 50
        val log by Log()
    }
}
