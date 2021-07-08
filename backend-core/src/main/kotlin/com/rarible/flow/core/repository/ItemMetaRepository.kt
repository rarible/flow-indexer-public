package com.rarible.flow.core.repository;

import com.rarible.flow.core.domain.ItemMeta
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

class ItemMetaRepository(
    private val mongo: ReactiveMongoTemplate
) {
    suspend fun save(itemMeta: ItemMeta): ItemMeta? {
        return mongo.save(itemMeta).awaitSingleOrNull()
    }

    suspend fun findByItemId(itemId: String): ItemMeta? {
        return mongo.find<ItemMeta>(
            Query(
                ItemMeta::itemId isEqualTo itemId
            )
        ).awaitFirstOrNull()
    }
}