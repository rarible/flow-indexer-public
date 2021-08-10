package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface OrderRepository: ReactiveMongoRepository<Order, Long> {
    fun findByItemId(itemId: ItemId): Mono<Order>
    fun deleteByItemId(itemId: ItemId): Mono<Order>
    fun findAllByMakerAndTakerIsNull(maker: FlowAddress): Flux<Order>
    fun findAllByTakerIsNull(): Flux<Order>
    fun findAllByTakerIsNullAndCollection(collection: String): Flux<Order>
}
