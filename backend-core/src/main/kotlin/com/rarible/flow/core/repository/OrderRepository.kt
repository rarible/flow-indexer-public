package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

interface OrderRepository: ReactiveMongoRepository<Order, Long>, OrderRepositoryCustom {
    @Query("""
        {"_id": ?0, "cancelled": false}
    """)
    fun findActiveById(id: Long): Mono<Order>

    fun findAllByIdIn(ids: List<Long>): Flux<Order>

    @Query("""
        {"make.contract": ?0, "make.tokenId": ?1}
    """)
    fun findAllByMake(contract: String, tokenId: Long): Flux<Order>

    fun findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
        make: FlowAsset,
        maker: FlowAddress,
        status: OrderStatus,
        lastUpdatedAt: LocalDateTime,
    ): Flux<Order>
}

interface OrderRepositoryCustom {
    fun search(filter: OrderFilter, cont: String?, limit: Int?, sort: OrderFilter.Sort = OrderFilter.Sort.LATEST_FIRST): Flux<Order>
}

@Suppress("unused")
class OrderRepositoryCustomImpl(val mongo: ReactiveMongoTemplate): OrderRepositoryCustom {
    override fun search(filter: OrderFilter, cont: String?, limit: Int?, sort: OrderFilter.Sort): Flux<Order> {
        val query = sort.scroll(filter, cont, limit)
        return mongo.find(query)
    }
}
