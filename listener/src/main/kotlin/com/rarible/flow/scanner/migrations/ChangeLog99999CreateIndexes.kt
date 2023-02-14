package com.rarible.flow.scanner.migrations

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.flow.core.repository.FlowLogEventIndexes
import com.rarible.flow.core.repository.ItemHistoryIndexes
import com.rarible.flow.core.repository.ItemIndexes
import com.rarible.flow.core.repository.OrderIndexes
import com.rarible.flow.core.repository.OwnershipIndexes
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "99999")
class ChangeLog99999CreateIndexes {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(
        id = "ChangeLog99999CreateIndexes.createIndicesForAllCollections",
        order = "99999",
        author = "protocol",
        runAlways = true
    )
    fun createIndicesForAllCollections(
        @NonLockGuarded mongo: ReactiveMongoTemplate,
    ) = runBlocking {
        logger.info("Checking missing indices")
        ItemHistoryIndexes.createIndexes(mongo)
        ItemIndexes.createIndexes(mongo)
        OwnershipIndexes.createIndexes(mongo)
        FlowLogEventIndexes.createIndexes(mongo)
        OrderIndexes.createIndexes(mongo)
        logger.info("All missing indices launched to be created in background")
    }
}
