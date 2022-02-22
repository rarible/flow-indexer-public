package com.rarible.flow.scanner.migrations

import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.scanner.service.CollectionService
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.runBlocking

@ChangeUnit(
    id = "ChangeLog00028RestartJambb2",
    order = "00028",
    author = "flow"
)
class ChangeLog00028RestartJambb2(
    private val collectionService: CollectionService,
    private val appProperties: AppProperties
) {

    @Execution
    fun changeSet() {
        runBlocking {
            collectionService.purgeCollectionHistory(Contracts.JAMBB_MOMENTS, appProperties.chainId)
        }
    }

    @RollbackExecution
    fun rollBack() {
    }
}
