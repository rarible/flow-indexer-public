package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo


class ItemRepository(
    private val mongo: ReactiveMongoTemplate
) {
    fun findAll(): Flow<Item> {
        return mongo.findAll<Item>().asFlow()
    }

    suspend fun findById(id: String): Item? {
        return mongo.findById<Item>(id).awaitFirstOrNull()
    }

    fun findAllByAccount(account: String): Flow<Item> {
        return findAll(
            Item::owner isEqualTo Address(account)
        )
    }

    fun findAllListed(): Flow<Item> {
        return findAll(
            Item::listed isEqualTo true
        )
    }

    suspend fun delete(id: String): Item? {
        return mongo.findAndRemove<Item>(
            Query.query(
                Item::id isEqualTo id
            )
        ).awaitFirstOrNull()
    }

    suspend fun save(item: Item): Item? {
        return mongo.save(item).awaitFirst();
    }

    fun findAllByCreator(address: String): Flow<Item> {
        return findAll(
            Item::creator isEqualTo Address(address)
        )
    }

    private fun findAll(criteria: Criteria): Flow<Item> {
        return mongo.find<Item>(
            Query.query(criteria)
        ).asFlow()
    }
}
