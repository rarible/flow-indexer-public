package com.rarible.flow.scanner.migrations

import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.scanner.service.CollectionService
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution

@ChangeUnit(
    id = "ChangeLog00047PurgeTopshots",
    order = "00047",
    author = "flow"
)
class ChangeLog00047PurgeTopshots(
    private val collectionService: CollectionService,
    private val appProperties: AppProperties
) {

    @Execution
    fun changeSet() {
        collectionService
            .purgeCollectionAsync(Contracts.TOPSHOT, appProperties.chainId)
            .blockLast()
    }

    @RollbackExecution
    fun rollBack() {
    }
}
