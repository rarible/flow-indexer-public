package com.rarible.flow.scanner.migrations

import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.scanner.service.CollectionService
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution

@ChangeUnit(
    id = "ChangeLog00048RestartFanfare",
    order = "00048",
    author = "flow"
)
class ChangeLog00048RestartFanfare(
    private val collectionService: CollectionService,
    private val appProperties: AppProperties
) {

    @Execution
    fun changeSet() {
        collectionService
            .purgeCollectionAsync(Contracts.FANFARE, appProperties.chainId)
            .blockLast()

        collectionService
            .restartDescriptorAsync(Contracts.FANFARE, 22741361L)
            .block()
    }

    @RollbackExecution
    fun rollBack() {
    }
}
