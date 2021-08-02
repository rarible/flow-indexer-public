package com.rarible.flow.core.repository

import com.mongodb.client.result.UpdateResult
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.log.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import org.onflow.sdk.FlowAddress
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.*
import org.springframework.data.mongodb.core.updateFirst
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface ItemRepository: ReactiveMongoRepository<Item, ItemId>, ItemRepositoryCustom {

    fun findAllByCreator(creator: FlowAddress): Flux<Item>

    fun findAllByListedIsTrue(): Flux<Item>

    fun findAllByIdIn(ids: List<ItemId>): Flux<Item>
}

interface ItemRepositoryCustom: ContinuationRepositoryCustom<Item, ItemFilter> {
    suspend fun markDeleted(itemId: ItemId): UpdateResult?
    suspend fun unlist(itemId: ItemId): UpdateResult
}

@Suppress("unused")
class ItemRepositoryCustomImpl(
    private val mongo: ReactiveMongoTemplate
): ItemRepositoryCustom {

    override suspend fun markDeleted(itemId: ItemId): UpdateResult {
        return update(itemId, Update().set(Item::deleted.name, true))
    }

    override suspend fun unlist(itemId: ItemId): UpdateResult {
        return update(itemId, Update().set(Item::listed.name, false))
    }

    override fun search(filter: ItemFilter, cont: Continuation?, limit: Int?): Flow<Item> {
        val criteria = when (filter) {
            is ItemFilter.All -> all()
            is ItemFilter.ByCreator -> byCreator(filter.creator)
            is ItemFilter.ByOwner -> byOwner(filter.owner)
            is ItemFilter.ByCollection -> byCollection(filter.collectionId)
        } scrollTo cont

        val query = Query.query(criteria).with(
            mongoSort(filter.sort)
        ).limit(limit ?: DEFAULT_LIMIT)

        return mongo.find<Item>(query).asFlow()
    }

    private fun all(): Criteria = Item::deleted isEqualTo false

    private fun byOwner(owner: FlowAddress): Criteria {
        return (Item::owner isEqualTo owner).andOperator(all())
    }

    private fun byCreator(creator: FlowAddress): Criteria {
        return (Item::creator isEqualTo creator).andOperator(all())
    }

    private fun byCollection(collectionId: String): Criteria =
        (Item::collection isEqualTo collectionId).andOperator(all())

    private fun mongoSort(sort: ItemFilter.Sort?): Sort {
        return when (sort) {
            ItemFilter.Sort.LAST_UPDATE -> Sort.by(
                Sort.Order.desc(Item::date.name),
                Sort.Order.desc(Item::id.name)
            )
            else -> Sort.unsorted()
        }
    }

    private infix fun Criteria.scrollTo(continuation: Continuation?): Criteria =
        if (continuation == null) {
            this
        } else {
            this.orOperator(
                Item::date lt continuation.afterDate,
                Criteria().andOperator(
                    Item::date isEqualTo continuation.afterDate,
                    Item::id lt continuation.afterId
                )
            )
        }

    private suspend fun update(
        itemId: ItemId,
        update: Update
    ): UpdateResult {
        return mongo.updateFirst<Item>(
            Query(Item::id isEqualTo itemId),
            update
        ).awaitFirstOrDefault(UpdateResult.unacknowledged())
    }

    companion object {
        const val DEFAULT_LIMIT: Int = 50
        val log by Log()
    }
}
