package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.LegacyOrder
import com.rarible.flow.core.domain.Order
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.BsonString
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import java.util.concurrent.atomic.AtomicInteger

@Deprecated("Delete after the migration")
interface LegacyOrderRepository : ReactiveMongoRepository<LegacyOrder, Long>, LegacyOrderRepositoryCustom {

    fun findAllByIdIn(ids: List<Long>): Flux<LegacyOrder>
}

interface LegacyOrderRepositoryCustom {

    suspend fun updateIdType()
}

@Suppress("unused")
class LegacyOrderRepositoryCustomImpl(val mongo: ReactiveMongoTemplate) : LegacyOrderRepositoryCustom {

    private val logger = LoggerFactory.getLogger(LegacyOrderRepository::class.java)

    override suspend fun updateIdType() {
        val counter = AtomicInteger()
        mongo.findAll(Document::class.java, Order.COLLECTION).asFlow().map { order ->
            val id = order.get("_id")!!
            if (id is Long) {
                mongo.remove(BsonDocument("_id", BsonInt64(id)), Order.COLLECTION).awaitSingleOrNull()
                order.put("_id", BsonString(id.toString()))
                mongo.insert(order, Order.COLLECTION).awaitSingle()
            }
            val updated = counter.incrementAndGet()
            if (updated % 1000 == 0) {
                logger.info("Order's IDs updated: $updated")
            }
        }.collect()
    }
}
