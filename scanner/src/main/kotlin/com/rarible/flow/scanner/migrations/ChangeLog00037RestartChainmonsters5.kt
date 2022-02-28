package com.rarible.flow.scanner.migrations

import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.scanner.service.CollectionService
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.runBlocking

@ChangeUnit(
    id = "ChangeLog00037RestartChainmonsters5",
    order = "00037",
    author = "flow"
)
class ChangeLog00037RestartChainmonsters5(
    private val collectionService: CollectionService,
    private val appProperties: AppProperties
) {

    @Execution
    fun changeSet() {
        runBlocking {
            collectionService.purgeCollectionHistory(Contracts.CHAINMONSTERS, appProperties.chainId, 11283560L)
        }
    }

    @RollbackExecution
    fun rollBack() {
    }
}