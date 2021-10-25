package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.data.mapping.div
import org.springframework.data.mapping.toDotPath
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface OrderRepository: ReactiveMongoRepository<Order, Long>, OrderRepositoryCustom {
    fun findByItemId(itemId: ItemId): Mono<Order>
    @Query("""
        {"_id": ?0, "cancelled": false}
    """)
    fun findActiveById(id: Long): Mono<Order>

    fun findAllByIdIn(ids: List<Long>): Flux<Order>

    fun findAllByMakeAndStatus(make: FlowAsset, status: OrderStatus): Flux<Order>
}

interface OrderRepositoryCustom {
    fun search(filter: OrderFilter, cont: String?, limit: Int?, sort: OrderFilter.Sort = OrderFilter.Sort.LAST_UPDATE): Flow<Order>
}

@Suppress("unused")
class OrderRepositoryCustomImpl(val mongo: ReactiveMongoTemplate): OrderRepositoryCustom {
    override fun search(filter: OrderFilter, cont: String?, limit: Int?, sort: OrderFilter.Sort): Flow<Order> {
        val query = filter.toQuery(cont, limit, sort)
        return mongo.find<Order>(query).asFlow()
    }
}
