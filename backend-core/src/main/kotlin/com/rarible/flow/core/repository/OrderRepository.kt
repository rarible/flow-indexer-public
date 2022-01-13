package com.rarible.flow.core.repository

import com.mongodb.client.result.UpdateResult
import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.filters.ScrollingSort
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.UpdateDefinition
import org.springframework.data.mongodb.core.update
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import java.time.LocalDateTime

interface OrderRepository: ReactiveMongoRepository<Order, Long>, OrderRepositoryCustom {

    fun findAllByIdIn(ids: List<Long>): Flux<Order>

    @Query("""
        {"make.contract": ?0, "make.tokenId": ?1}
    """)
    fun findAllByMake(contract: String, tokenId: Long): Flux<Order>

    @Query("""
        {"take.contract": ?0, "take.tokenId": ?1}
    """)
    fun findAllByTake(contract: String, tokenId: Long): Flux<Order>

    fun findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
        make: FlowAsset,
        maker: FlowAddress,
        status: OrderStatus,
        lastUpdatedAt: LocalDateTime,
    ): Flux<Order>

    fun findAllByStatus(status: OrderStatus): Flux<Order>
}

interface OrderRepositoryCustom: ScrollingRepository<Order> {

    suspend fun update(filter: OrderFilter, updateDefinition: UpdateDefinition): UpdateResult
}

@Suppress("unused")
class OrderRepositoryCustomImpl(val mongo: ReactiveMongoTemplate): OrderRepositoryCustom {
    override fun defaultSort(): ScrollingSort<Order> {
        return OrderFilter.Sort.LATEST_FIRST
    }

    override fun findByQuery(query: org.springframework.data.mongodb.core.query.Query): Flux<Order> {
        return mongo.find(query)
    }

    override suspend fun update(filter: OrderFilter, updateDefinition: UpdateDefinition): UpdateResult {
        return mongo
            .update<Order>()
            .matching(filter.criteria())
            .apply(updateDefinition)
            .all()
            .awaitSingle()
    }
}
