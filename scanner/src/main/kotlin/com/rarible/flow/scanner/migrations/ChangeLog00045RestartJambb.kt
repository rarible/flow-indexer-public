package com.rarible.flow.scanner.migrations

import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.scanner.service.CollectionService
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution

@ChangeUnit(
    id = "ChangeLog00045RestartJambb",
    order = "00045",
    author = "flow"
)
class ChangeLog00045RestartJambb(
    private val collectionService: CollectionService,
    private val appProperties: AppProperties
) {

    @Execution
    fun changeSet() {
        collectionService
            .purgeCollectionAsync(Contracts.JAMBB_MOMENTS, appProperties.chainId)
            .blockLast()

        collectionService
            .restartDescriptorAsync(Contracts.JAMBB_MOMENTS, 20445936L)
            .block()
    }

    @RollbackExecution
    fun rollBack() {
    }
}
