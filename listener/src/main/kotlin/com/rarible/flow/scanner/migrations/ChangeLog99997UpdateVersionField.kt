package com.rarible.flow.scanner.migrations

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.Ownership
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@ChangeLog(order = "99997")
class ChangeLog99997UpdateVersionField {

    @ChangeSet(
        id = "ChangeLog99997UpdateVersionField.updateVersion",
        order = "99997",
        author = "protocol"
    )
    fun updateVersion(@NonLockGuarded mongo: ReactiveMongoTemplate) = runBlocking {
        listOf(
            Item.COLLECTION,
            Ownership.COLLECTION,
            Order.COLLECTION
        ).forEach {
            val updateResult = mongo.updateMulti(
                Query(Criteria("version").exists(false)),
                Update().set("version", 0),
                it
            ).awaitSingle()

            logger.info("Updated version field for $it {}/{}", updateResult.modifiedCount, updateResult.matchedCount)
        }
    }

    private val logger = LoggerFactory.getLogger(javaClass)
}
