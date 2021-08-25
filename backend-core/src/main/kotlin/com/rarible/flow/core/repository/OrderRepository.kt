package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDateTime

interface OrderRepository: ReactiveMongoRepository<Order, Long> {
    fun findByItemId(itemId: ItemId): Mono<Order>
    fun deleteByItemId(itemId: ItemId): Mono<Order>
    fun findAllByMakerAndTakerIsNullOrderByCreatedAtDesc(maker: FlowAddress): Flux<Order>
    fun findAllByMakerAndCreatedAtAfterAndTakerIsNullAndIdAfterOrderByCreatedAtDesc(maker: FlowAddress, createdAt: LocalDateTime, id: Long): Flux<Order>
    fun findAllByTakerIsNullOrderByCreatedAtDesc(): Flux<Order>
    fun findAllByTakerIsNullAndCreatedAtAfterAndIdGreaterThanOrderByCreatedAtDesc(createdAt: LocalDateTime, id: Long): Flux<Order>
    fun findAllByTakerIsNullAndCollectionOrderByCreatedAtDesc(collection: String): Flux<Order>
    fun findAllByTakerIsNullAndCollectionAndCreatedAtAfterAndIdAfterOrderByCreatedAtDesc(collection: String, afterDateTime: LocalDateTime, id: Long): Flux<Order>
    @Query("""
        {"_id": ?0, "cancelled": false, "fill": 0}
    """)
    fun findActiveById(id: Long): Mono<Order>
}