package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findAll


class ItemRepository(
    private val mongo: ReactiveMongoTemplate
)  {
    fun findAll(): Flow<Item> {
        return mongo.findAll<Item>().asFlow()
    }
}