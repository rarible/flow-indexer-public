package com.rarible.flow.scanner.migrations

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.flow.core.repository.LegacyOrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

@ChangeLog(order = "00001")
class ChangeLog00001UpdateOrderIdType {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(
        id = "ChangeLog00001UpdateOrderIdType.updateIdType",
        order = "00001",
        author = "protocol"
    )
    fun updateIdType(
        @NonLockGuarded legacyRepository: LegacyOrderRepository,
    ) = runBlocking {
        logger.info("Update Order._id type started")
        legacyRepository.updateIdType()
        logger.info("All unused indices has been dropped")
    }
}
