package com.rarible.flow.core.repository

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.FlowLogEvent
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index

object FlowLogEventIndexes {

    suspend fun createIndexes(mongo: ReactiveMongoTemplate) {
        ALL_INDEXES.forEach { mongo.indexOps(FlowLogEvent.COLLECTION).ensureIndex(it).awaitFirst() }
    }

    // TODO ChangeLog00052OrderIndexByDbUpdatedAtAndIdField - Looks OK
    private val BY_DB_UPDATE_AT: Index = Index()
        .on("${FlowLogEvent::log.name}.${FlowLog::eventType.name}", Sort.Direction.ASC)
        .named("flow_log_event_type")
        .background()

    private val BY_TRANSACTION_HASH_AND_EVENT_INDEX: Index = Index()
        .on("${FlowLogEvent::log.name}.${FlowLog::transactionHash.name}", Sort.Direction.ASC)
        .on("${FlowLogEvent::log.name}.${FlowLog::eventIndex.name}", Sort.Direction.ASC)
        .background()

    private val ALL_INDEXES = listOf(
        BY_DB_UPDATE_AT,
        BY_TRANSACTION_HASH_AND_EVENT_INDEX,
    )
}