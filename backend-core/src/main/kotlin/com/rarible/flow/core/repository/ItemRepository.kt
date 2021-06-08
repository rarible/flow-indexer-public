package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
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
        return mongo.findById<Item>(id).awaitFirst()
    }

    suspend fun save(item: Item): Item? {
        return mongo.save(item).awaitFirst();
    }

}