package com.rarible.flow.scanner.migrations

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.service.CollectionService

//@ChangeUnit(
//    id = "ChangeLog00043RestartChainmonstersTask",
//    order = "00043",
//    author = "flow"
//)
//class ChangeLog00043RestartChainmonstersTask(
//    private val collectionService: CollectionService
//) {
//
//    @Execution
//    fun changeSet() {
//        collectionService
//            .restartDescriptorAsync(Contracts.CHAINMONSTERS, 12020060L)
//            .block()
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//    }
//}
