package com.rarible.flow.scanner.migrations

import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.scanner.service.CollectionService

//@ChangeUnit(
//    id = "ChangeLog00041RestartChainmonsters",
//    order = "00041",
//    author = "flow"
//)
//class ChangeLog00041RestartChainmonsters(
//    private val collectionService: CollectionService,
//    private val appProperties: AppProperties
//) {
//
//    @Execution
//    fun changeSet() {
//        collectionService
//            .purgeCollectionAsync(Contracts.CHAINMONSTERS, appProperties.chainId)
//            .blockLast()
//
//        collectionService
//            .restartDescriptorAsync(Contracts.CHAINMONSTERS, 11283560L)
//            .block()
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//    }
//}
