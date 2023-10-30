package com.rarible.flow.core.repository

import com.mongodb.client.result.UpdateResult
import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.filters.ScrollingSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.UpdateDefinition
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.query.lte
import org.springframework.data.mongodb.core.update
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import java.time.Instant
import java.time.LocalDateTime

interface OrderRepository : ReactiveMongoRepository<Order, String>, OrderRepositoryCustom {

    fun findAllByIdIn(ids: List<String>): Flux<Order>

    @Query(
        """
        {"make.contract": ?0, "make.tokenId": ?1}
    """
    )
    fun findAllByMake(contract: String, tokenId: Long): Flux<Order>

    fun findByItemId(itemId: ItemId): Flux<Order>

    fun deleteByItemId(itemId: ItemId): Flux<Order>

    @Query(
        """
        {"take.contract": ?0, "take.tokenId": ?1}
    """
    )
    fun findAllByTake(contract: String, tokenId: Long): Flux<Order>

    fun findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
        make: FlowAsset,
        maker: FlowAddress,
        status: OrderStatus,
        lastUpdatedAt: LocalDateTime,
    ): Flux<Order>

    fun findAllByStatus(status: OrderStatus): Flux<Order>
}

interface OrderRepositoryCustom : ScrollingRepository<Order> {

    suspend fun update(filter: OrderFilter, updateDefinition: UpdateDefinition): UpdateResult
    fun findExpiredOrders(now: Instant): Flow<Order>
    fun findNotStartedOrders(now: Instant): Flow<Order>
}

@Suppress("unused")
class OrderRepositoryCustomImpl(val mongo: ReactiveMongoTemplate) : OrderRepositoryCustom {
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

    override fun findExpiredOrders(now: Instant): Flow<Order> {
        val query = org.springframework.data.mongodb.core.query.Query(
            Criteria().andOperator(
                Order::status isEqualTo OrderStatus.ACTIVE,
                Order::end exists true,
                Order::end gt 0,
                Order::end lt now.epochSecond
            )
        )
        return mongo.query<Order>().matching(query).all().asFlow()
    }

    override fun findNotStartedOrders(now: Instant): Flow<Order> {
        val query = org.springframework.data.mongodb.core.query.Query(
            Criteria().andOperator(
                Order::status isEqualTo OrderStatus.INACTIVE,
                Criteria().orOperator(
                    Criteria().orOperator(
                        Order::end exists false,
                        Order::end isEqualTo 0,
                    ),
                    Criteria().andOperator(
                        Order::end exists true,
                        Order::end gte now.epochSecond
                    )
                ),
                Criteria().orOperator(
                    Order::start exists false,
                    Criteria().andOperator(
                        Order::start exists true,
                        Order::start lte now.epochSecond
                    )
                )
            )
        )
        return mongo.query<Order>().matching(query).all().asFlow()
    }
}
