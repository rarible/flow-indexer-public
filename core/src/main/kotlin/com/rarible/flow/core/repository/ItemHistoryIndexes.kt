package com.rarible.flow.core.repository

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowNftActivity
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.NFTActivity
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.domain.TypedFlowActivity
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index

object ItemHistoryIndexes {

    suspend fun createIndexes(mongo: ReactiveMongoTemplate) {
        ALL_INDEXES.forEach { mongo.indexOps(ItemHistory.COLLECTION).ensureIndex(it).awaitFirst() }
    }

    // TODO taken from annotation, ensure it is really needed
    private val ACTIVITY_SIBLINGS_INDEX: Index = Index()
        .on("${ItemHistory::activity.name}.${TypedFlowActivity::type.name}", Sort.Direction.ASC)
        .on("${ItemHistory::activity.name}.${NFTActivity::contract.name}", Sort.Direction.ASC)
        .on("${ItemHistory::activity.name}.${NFTActivity::tokenId.name}", Sort.Direction.ASC)
        .on("${ItemHistory::log.name}.${FlowLog::transactionHash.name}", Sort.Direction.ASC)
        .on("${ItemHistory::log.name}.${FlowLog::eventIndex.name}", Sort.Direction.ASC)
        .named("activity_siblings")
        .background()

    // TODO taken from annotation, ensure it is really needed
    private val LOG_UNIQUE: Index = Index()
        .on("${ItemHistory::log.name}.${FlowLog::transactionHash.name}", Sort.Direction.ASC)
        .on("${ItemHistory::log.name}.${FlowLog::eventIndex.name}", Sort.Direction.ASC)
        .named("log_uniq")
        .unique()
        .background()

    private val ACTIVITY_FROM: Index = Index()
        .on("${ItemHistory::activity.name}.${TransferActivity::from.name}", Sort.Direction.ASC)
        .named("activity_from")
        .background()

    private val ACTIVITY_TO: Index = Index()
        .on("${ItemHistory::activity.name}.${TransferActivity::to.name}", Sort.Direction.ASC)
        .named("activity_to")
        .background()

    private val ACTIVITY_TIMESTAMP: Index = Index()
        .on("${ItemHistory::activity.name}.${BaseActivity::timestamp.name}", Sort.Direction.ASC)
        .named("activity_timestamp")
        .background()

    private val ACTIVITY_OWNER: Index = Index()
        .on("${ItemHistory::activity.name}.${FlowNftActivity::owner.name}", Sort.Direction.ASC)
        .named("activity_owner")
        .background()

    private val ACTIVITY_MAKER: Index = Index()
        .on("${ItemHistory::activity.name}.maker", Sort.Direction.ASC)
        .named("activity_maker")
        .background()

    private val DEFAULT_ORDER_INDEX: Index = Index()
        .on(ItemHistory::date.name, Sort.Direction.DESC)
        .on("${ItemHistory::log.name}.${FlowLog::transactionHash.name}", Sort.Direction.DESC)
        .on("${ItemHistory::log.name}.${FlowLog::eventIndex.name}", Sort.Direction.DESC)
        .named("default_order_idx")
        .background()

    private val TX_TYPE_LEFT_MAKER_RIGHT_MAKER: Index = Index()
        .on("${ItemHistory::log.name}.${FlowLog::transactionHash.name}", Sort.Direction.ASC)
        .on("${ItemHistory::activity.name}.${TypedFlowActivity::type.name}", Sort.Direction.ASC)
        .on("${ItemHistory::activity.name}.left.maker", Sort.Direction.ASC)
        .on("${ItemHistory::activity.name}.right.maker", Sort.Direction.ASC)
        .named("tx_type_left_right")
        .background()

    private val TX_TYPE_FROM_TO: Index = Index()
        .on("log.transactionHash", Sort.Direction.ASC)
        .on("${ItemHistory::activity.name}.${TypedFlowActivity::type.name}", Sort.Direction.ASC)
        .on("${ItemHistory::activity.name}.${TransferActivity::from.name}", Sort.Direction.ASC)
        .on("${ItemHistory::activity.name}.${TransferActivity::to.name}", Sort.Direction.ASC)
        .named("tx_type_from_to")
        .background()

    // For SYNC requests
    private val BY_UPDATED_AT: Index = Index()
        .on(ItemHistory::updatedAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val BY_TYPE_UPDATED_AT: Index = Index()
        .on("${ItemHistory::activity.name}.${TypedFlowActivity::type.name}", Sort.Direction.ASC)
        .on(ItemHistory::updatedAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val ALL_INDEXES = listOf(
        // Since we are using ES, these indices are only we need (except scanner's internal indices)
        BY_UPDATED_AT,
        BY_TYPE_UPDATED_AT,

        // TODO check if these indexes are really needed
        ACTIVITY_SIBLINGS_INDEX,
        LOG_UNIQUE,
        // ChangeLog00003ItemHistoryIndex - looks useless (all 4)
        ACTIVITY_FROM,
        ACTIVITY_TO,
        ACTIVITY_TIMESTAMP,
        ACTIVITY_OWNER,
        // ChangeLog00004ItemHistoryMakerIndex - looks useless
        ACTIVITY_MAKER,
        // ChangeLog00006AddItemHistorySortIndex
        DEFAULT_ORDER_INDEX,
        // ChangeLog00049ItemHistoryIndexes
        TX_TYPE_LEFT_MAKER_RIGHT_MAKER,
        TX_TYPE_FROM_TO,

        )

}
