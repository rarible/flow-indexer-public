package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.Order
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria

object OrderIndexes {

    suspend fun createIndexes(mongo: ReactiveMongoTemplate) {
        ALL_INDEXES.forEach { mongo.indexOps(Order.COLLECTION).ensureIndex(it).awaitFirst() }
    }

    private val sellPartialIndexCriteria = PartialIndexFilter.of(
        Criteria("${Order::make.name}.${FlowAssetNFT::tokenId.name}").exists(true)
    )

    private val bidPartialIndexCriteria = PartialIndexFilter.of(
        Criteria("${Order::take.name}.${FlowAssetNFT::tokenId.name}").exists(true)
    )

    private val BY_DB_UPDATE_AT: Index = Index()
        .on(Order::dbUpdatedAt.name, Sort.Direction.ASC)
        .on(Order::id.name, Sort.Direction.ASC)
        .background()

    private val BY_UPDATED_AT: Index = Index()
        .on(Order::lastUpdatedAt.name, Sort.Direction.DESC)
        .on(Order::id.name, Sort.Direction.DESC)
        .background()

    private val BY_STATUS_UPDATED_AT: Index = Index()
        .on(Order::status.name, Sort.Direction.DESC)
        .on(Order::lastUpdatedAt.name, Sort.Direction.DESC)
        .on(Order::id.name, Sort.Direction.DESC)
        .background()

    private val BY_MAKER_UPDATED_AT: Index = Index()
        .on(Order::maker.name, Sort.Direction.DESC)
        .on(Order::lastUpdatedAt.name, Sort.Direction.DESC)
        .on(Order::id.name, Sort.Direction.DESC)
        .background()

    private val SELL_BY_COLLECTION_UPDATED_AT: Index = Index()
        .on(Order::collection.name, Sort.Direction.DESC)
        .on(Order::lastUpdatedAt.name, Sort.Direction.DESC)
        .on(Order::id.name, Sort.Direction.DESC)
        .partial(sellPartialIndexCriteria)
        .background()

    // For sell-currencies request
    private val SELL_BY_ITEM_PRICE: Index = Index()
        .on(Order::itemId.name, Sort.Direction.ASC)
        .on("${Order::take.name}.${FlowAsset::contract.name}", Sort.Direction.ASC)
        .partial(sellPartialIndexCriteria)
        .background()

    private val SELL_BY_ITEM_STATUS_CURRENCY_PRICE: Index = Index()
        .on(Order::itemId.name, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .on("${Order::take.name}.${FlowAsset::contract.name}", Sort.Direction.ASC)
        .on(Order::amount.name, Sort.Direction.ASC)
        .on(Order::id.name, Sort.Direction.ASC)
        .partial(sellPartialIndexCriteria)
        .background()

    // For bid-currencies request
    private val BID_BY_ITEM_PRICE: Index = Index()
        .on(Order::itemId.name, Sort.Direction.ASC)
        .on("${Order::make.name}.${FlowAsset::contract.name}", Sort.Direction.ASC)
        .partial(bidPartialIndexCriteria)
        .background()

    private val BID_BY_ITEM_STATUS_CURRENCY_PRICE: Index = Index()
        .on(Order::itemId.name, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .on("${Order::make.name}.${FlowAsset::contract.name}", Sort.Direction.ASC)
        .on(Order::amount.name, Sort.Direction.ASC)
        .on(Order::id.name, Sort.Direction.ASC)
        .partial(bidPartialIndexCriteria)
        .background()

    private val STATUS_END_START: Index = Index()
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::end.name, Sort.Direction.ASC)
        .on(Order::start.name, Sort.Direction.ASC)
        .background()

    private val BY_MAKE: Index = Index()
        .on("${Order::make.name}.${FlowAssetNFT::contract.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${FlowAssetNFT::tokenId.name}", Sort.Direction.ASC)
        .background()

    private val BY_TAKE: Index = Index()
        .on("${Order::take.name}.${FlowAssetNFT::contract.name}", Sort.Direction.ASC)
        .on("${Order::take.name}.${FlowAssetNFT::tokenId.name}", Sort.Direction.ASC)
        .background()

    private val ALL_INDEXES = listOf(
        BY_UPDATED_AT,
        BY_DB_UPDATE_AT,
        BY_STATUS_UPDATED_AT,
        BY_MAKER_UPDATED_AT,

        SELL_BY_COLLECTION_UPDATED_AT,
        SELL_BY_ITEM_PRICE,
        SELL_BY_ITEM_STATUS_CURRENCY_PRICE,
        BY_MAKE,
        BY_TAKE,

        BID_BY_ITEM_PRICE,
        BID_BY_ITEM_STATUS_CURRENCY_PRICE,

        STATUS_END_START
    )
}
