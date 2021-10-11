package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

interface OrderRepository: ReactiveMongoRepository<Order, Long>, OrderRepositoryCustom {
    fun findByItemId(itemId: ItemId): Mono<Order>
    @Query("""
        {"_id": ?0, "cancelled": false, "fill": "0"}
    """)
    fun findActiveById(id: Long): Mono<Order>
}

interface OrderRepositoryCustom: ContinuationRepositoryCustom<Order, OrderFilter>

@Suppress("unused")
class OrderRepositoryCustomImpl(val mongo: ReactiveMongoTemplate): OrderRepositoryCustom {
    override fun search(filter: OrderFilter, cont: Continuation?, limit: Int?): Flow<Order> {
        cont as ActivityContinuation?
        val criteria = filter.criteria() scrollTo cont
        val query = org.springframework.data.mongodb.core.query.Query.query(criteria).with(
            mongoSort(filter.sort)
        ).limit(limit ?: ItemRepositoryCustomImpl.DEFAULT_LIMIT)

        return mongo.find<Order>(query).asFlow()
    }

    private fun mongoSort(sort: OrderFilter.Sort?): Sort {
        return when (sort) {
            OrderFilter.Sort.LAST_UPDATE -> Sort.by(
                Sort.Order.desc(Order::createdAt.name),
                Sort.Order.desc(Order::id.name)
            )
            else -> Sort.unsorted()
        }
    }

    private infix fun Criteria.scrollTo(continuation: ActivityContinuation?): Criteria =
        if (continuation == null) {
            this
        } else {
            val lastDate = LocalDateTime.ofInstant(continuation.beforeDate, ZoneOffset.UTC)
            this.orOperator(
                Order::createdAt lt lastDate,
                Criteria().andOperator(
                    Order::createdAt isEqualTo lastDate,
                    Order::id lt continuation.beforeId
                )
            )
        }


}
