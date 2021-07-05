package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.*
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

    suspend fun findById(contract: Address, tokenId: Int): Item? {
        return findById(Item.makeId(contract, tokenId))
    }

    fun findAllByAccount(account: String): Flow<Item> {
        return mongo.find<Item>(
            Query.query(
                Item::owner isEqualTo Address(account)
            )
        ).asFlow()
    }

    fun findAllListed(): Flow<Item> {
        return mongo.find<Item>(
            Query.query(
                Item::listed isEqualTo true
            )
        ).asFlow()
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
}