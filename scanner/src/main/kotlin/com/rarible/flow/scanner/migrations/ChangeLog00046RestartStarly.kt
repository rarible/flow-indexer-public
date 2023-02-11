package com.rarible.flow.scanner.migrations

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.scanner.service.CollectionService

//@ChangeUnit(
//    id = "ChangeLog00046RestartStarly",
//    order = "00046",
//    author = "flow"
//)
//class ChangeLog00046RestartStarly(
//    private val collectionService: CollectionService,
//    private val appProperties: AppProperties
//) {
//
//    @Execution
//    fun changeSet() {
//        if (appProperties.chainId == FlowChainId.MAINNET) {
//            collectionService
//                .purgeCollectionAsync(Contracts.STARLY_CARD, appProperties.chainId)
//                .blockLast()
//
//            collectionService
//                .restartDescriptorAsync(Contracts.STARLY_CARD, 18133134L)
//                .block()
//        }
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//    }
//}
