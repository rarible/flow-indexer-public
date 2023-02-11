package com.rarible.flow.scanner.migrations

import com.rarible.core.task.Task
import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.scanner.service.CollectionService
import com.rarible.flow.scanner.subscriber.flowDescriptorName
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove

//@ChangeUnit(
//    id = "ChangeLog00047PurgeTopshots",
//    order = "00047",
//    author = "flow"
//)
//class ChangeLog00047PurgeTopshots(
//    private val collectionService: CollectionService,
//    private val appProperties: AppProperties,
//    private val mongoTemplate: MongoTemplate
//) {
//
//    @Execution
//    fun changeSet() {
//        collectionService
//            .purgeCollectionAsync(Contracts.TOPSHOT, appProperties.chainId)
//            .blockLast()
//
//        mongoTemplate.remove<Task>(
//            Query(Task::param isEqualTo Contracts.TOPSHOT.flowDescriptorName())
//        )
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//    }
//}
