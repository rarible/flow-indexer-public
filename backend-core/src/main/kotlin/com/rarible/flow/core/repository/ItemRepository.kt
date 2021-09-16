package com.rarible.flow.core.repository

import com.mongodb.client.result.UpdateResult
import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.log.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.*
import org.springframework.data.mongodb.core.updateFirst
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ItemRepository : ReactiveMongoRepository<Item, ItemId>, ItemRepositoryCustom, ReactiveQuerydslPredicateExecutor<Item> {

    fun findAllByCreator(creator: FlowAddress): Flux<Item>

    fun findAllByListedIsTrue(): Flux<Item>

    fun findByIdAndOwnerIsNotNullOrderByMintedAtDescTokenIdDesc(itemId: ItemId): Mono<Item>
}

interface ItemRepositoryCustom : ContinuationRepositoryCustom<Item, ItemFilter> {
    suspend fun updateById(itemId: ItemId, update: Update): UpdateResult
}

@Suppress("unused")
class ItemRepositoryCustomImpl(
    private val mongo: ReactiveMongoTemplate
) : ItemRepositoryCustom {

    override fun search(filter: ItemFilter, cont: Continuation?, limit: Int?): Flow<Item> {
        cont as NftItemContinuation?
        val criteria = filter.criteria() scrollTo cont
        val query = Query.query(criteria).with(
            mongoSort(filter.sort)
        ).limit(limit ?: DEFAULT_LIMIT)

        return mongo.find<Item>(query).asFlow()
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

    private infix fun Criteria.scrollTo(continuation: NftItemContinuation?): Criteria =
        if (continuation == null) {
            this
        } else {
            this.orOperator(
                Item::mintedAt lt continuation.afterDate,
                Criteria().andOperator(
                    Item::mintedAt isEqualTo continuation.afterDate,
                    Item::id lt continuation.afterId
                )
            )
        }

    override suspend fun updateById(
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
