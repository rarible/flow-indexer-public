package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.TokenId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono
import java.math.BigInteger

interface OrderRepositoryR: ReactiveMongoRepository<Order, ObjectId> {
    fun findByItemId(itemId: ItemId): Mono<Order>
    fun deleteByItemId(itemId: ItemId): Mono<Order>
}