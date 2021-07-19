package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.onflow.sdk.FlowAddress
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import java.math.BigInteger


class OrderRepository(
    private val mongo: ReactiveMongoTemplate
) {
    fun findAll(): Flow<Order> {
        return mongo.findAll<Order>().asFlow()
    }

    suspend fun findById(id: String): Order? {
        return mongo.findById<Order>(id).awaitFirstOrNull()
    }

    suspend fun findByItemId(contract: FlowAddress, tokenId: BigInteger): Order? {
        return mongo.find<Order>(
            Query.query(
                Order::itemId isEqualTo ItemId(contract, tokenId)
            )
        ).awaitFirstOrNull()
    }

    fun findAllByAccount(account: FlowAddress): Flow<Order> {
        return mongo.find<Order>(
            Query.query(
                Order::taker isEqualTo account
            )
        ).asFlow()
    }

    suspend fun delete(id: String): Order? {
        return mongo.findAndRemove<Order>(
            Query.query(
                Order::id isEqualTo id
            )
        ).awaitFirstOrNull()
    }

    suspend fun save(item: Order): Order? {
        return mongo.save(item).awaitFirst()
    }
}
