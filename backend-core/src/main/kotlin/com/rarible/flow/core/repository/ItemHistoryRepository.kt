package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.*
import com.rarible.protocol.dto.FlowAggregationDataDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lte
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant

/**
 * Item history repo
 */
@Repository
interface ItemHistoryRepository:
    ReactiveMongoRepository<ItemHistory, String>,
    ReactiveQuerydslPredicateExecutor<ItemHistory>,
    ItemHistoryRepositoryCustom {

        @Suppress("FunctionName")
        fun existsByLog_TransactionHashAndLog_EventIndex(txHash: String, eventIndex: Int): Mono<Boolean>
    }

interface ItemHistoryRepositoryCustom {
    fun aggregatePurchaseByCollection(
        start: Instant, end: Instant, size: Long?
    ): Flow<FlowAggregationDataDto>

    fun aggregatePurchaseByTaker(
        start: Instant, end: Instant, size: Long?
    ): Flow<FlowAggregationDataDto>

    fun aggregateSellByMaker(
        start: Instant, end: Instant, size: Long?
    ): Flow<FlowAggregationDataDto>
}

class ItemHistoryRepositoryCustomImpl(
    private val mongo: ReactiveMongoTemplate
) : ItemHistoryRepositoryCustom {
    override fun aggregatePurchaseByCollection(
        start: Instant,
        end: Instant,
        size: Long?
    ): Flow<FlowAggregationDataDto> {
        return getNftPurchaseAggregation(
            "${ItemHistory::activity.name}.${FlowNftOrderActivitySell::contract.name}",
            start,
            end
        )
    }

    override fun aggregatePurchaseByTaker(start: Instant, end: Instant, size: Long?): Flow<FlowAggregationDataDto> {
        return getNftPurchaseAggregation(
            "${ItemHistory::activity.name}.${FlowNftOrderActivitySell::right.name}.${OrderActivityMatchSide::maker.name}",
            start,
            end
        )
    }

    override fun aggregateSellByMaker(start: Instant, end: Instant, size: Long?): Flow<FlowAggregationDataDto> {
        return getNftPurchaseAggregation(
            "${ItemHistory::activity.name}.${FlowNftOrderActivitySell::left.name}.${OrderActivityMatchSide::maker.name}",
            start,
            end
        )
    }

    private fun getNftPurchaseAggregation(
        groupByField: String,
        startDate: Instant,
        endDate: Instant
    ): Flow<FlowAggregationDataDto> {
        val match = Aggregation.match(
            Criteria().andOperator(
                ItemHistory::date.gte(startDate),
                ItemHistory::date.lte(endDate),
                Criteria("${ItemHistory::activity.name}.${BaseActivity::type.name}").isEqualTo(FlowActivityType.SELL)
            )
        )
        val group = Aggregation
            .group(groupByField)
            .sum("activity.right.asset.value").`as`(FlowAggregationDataDto::sum.name)
            .count().`as`(FlowAggregationDataDto::count.name)

        val sort = Aggregation.sort(Sort.by(Sort.Direction.DESC, FlowAggregationDataDto::sum.name))
        val aggregation = Aggregation.newAggregation(match, group, sort)

        return mongo
            .aggregate(aggregation, "item_history", Agg::class.java)
            .asFlow()
            .map {
                FlowAggregationDataDto(it._id ?: "", it.sum, it.count)
            }
    }
}

data class Agg(val _id: String?, val sum: BigDecimal, val count: Long)
