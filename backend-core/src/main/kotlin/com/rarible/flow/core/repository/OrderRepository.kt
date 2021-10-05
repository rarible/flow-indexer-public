package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface OrderRepository: ReactiveMongoRepository<Order, Long> {
    fun findByItemId(itemId: ItemId): Mono<Order>
    fun findByItemIdAndCancelledAndMaker(itemId: ItemId, cancelled: Boolean, maker: FlowAddress): Mono<Order>
    @Query("""
        {"_id": ?0, "cancelled": false, "fill": "0"}
    """)
    fun findActiveById(id: Long): Mono<Order>
}
