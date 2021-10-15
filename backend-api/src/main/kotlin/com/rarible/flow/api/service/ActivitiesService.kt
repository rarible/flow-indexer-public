package com.rarible.flow.api.service

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.enum.safeOf
import com.rarible.protocol.dto.FlowActivitiesDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.time.Instant

@FlowPreview
@Service
class ActivitiesService(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository
) {
    suspend fun getNftOrderActivitiesByItem(
        types: List<FlowActivityType>,
        contract: String,
        tokenId: Long,
        continuation: ActivityContinuation?,
        size: Int?,
        sort: String? = "LATEST_FIRST"
    ): FlowActivitiesDto {
        val order = order(sort)
        var predicate = byTypes(types).and(byContractAndTokenPredicate(contract, tokenId))

        if (continuation != null) {
            predicate = predicate.and(byContinuation(continuation))
        }

        val flow = itemHistoryRepository.findAll(predicate, *order).asFlow()
        return flowActivitiesDto(flow, size)
    }

    suspend fun getNftOrderActivitiesByUser(
        type: List<String>,
        user: List<String>,
        continuation: String?,
        from: Instant?,
        to: Instant?,
        size: Int?,
        sort: String? = "LATEST_FIRST"
    ): FlowActivitiesDto {
        val haveTransferTo = type.isEmpty() || type.contains("TRANSFER_TO")
        val haveTransferFrom = type.isEmpty() || type.contains("TRANSFER_FROM")

        val types = if (type.isEmpty()) {
            FlowActivityType.values().toMutableList()
        } else {
            safeOf<FlowActivityType>(
                type.filter { "TRANSFER_TO" != it && "TRANSFER_FROM" != it }
            ).toMutableList()
        }
        val cont = ActivityContinuation.of(continuation)

        val order = order(sort)
        var predicate = BooleanBuilder()
        if (cont != null) {
            predicate = predicate.and(byContinuation(cont))
        }

        val transferFromActivities: Flow<ItemHistory> = if (haveTransferFrom) {
            itemHistoryRepository.findAll(transferFromPredicate(user, from, to), *order).asFlow()
        } else emptyFlow()


        val transferToActivities: Flow<ItemHistory> = if (haveTransferTo) {
            itemHistoryRepository.findAll(transferToPredicate(user, from, to), *order).asFlow()
        } else emptyFlow()

        val listActivities: Flow<ItemHistory> = if (types.contains(FlowActivityType.LIST)) {
            itemHistoryRepository.findAll(listPredicate(user, from, to), *order).asFlow()
        } else emptyFlow()
        types.remove(FlowActivityType.LIST)

        val sellActivities: Flow<ItemHistory> = if (types.contains(FlowActivityType.SELL)) {
            itemHistoryRepository.findAll(sellPredicate(user, from, to), *order).asFlow()
        } else emptyFlow()
        types.remove(FlowActivityType.SELL)

        if (haveTransferFrom || haveTransferTo) {
            types.remove(FlowActivityType.TRANSFER)
        }

        val activities: Flow<ItemHistory> = if (types.isEmpty()) emptyFlow() else itemHistoryRepository.findAll(
            predicate.and(byTypes(types)).and(byOwner(user)), *order
        ).asFlow()

        return flowActivitiesDto(
            flowOf(
                activities,
                transferFromActivities,
                transferToActivities,
                listActivities,
                sellActivities
            ).flattenConcat(), size
        )
    }

    private suspend fun flowActivitiesDto(
        flow: Flow<ItemHistory>,
        size: Int?
    ): FlowActivitiesDto {
        var result = flow
        if (size != null) {
            result = result.take(size)
        }

        val items = result.toList()

        return FlowActivitiesDto(
            items = items.map { h -> h.activity.toDto(h) },
            total = items.size,
            continuation = "${answerContinuation(items)}"
        )
    }

    suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?,
        sort: String? = "LATEST_FIRST"
    ): FlowActivitiesDto {
        val types = safeOf(type, FlowActivityType.values().toList())

        val cont = ActivityContinuation.of(continuation)
        val order = order(sort)
        val predicate = byTypes(types)

        if (cont != null) {
            predicate.and(byContinuation(cont))
        }
        return flowActivitiesDto(itemHistoryRepository.findAll(predicate, *order).asFlow(), size)
    }

    suspend fun getNfdOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: String? = "LATEST_FIRST"
    ): FlowActivitiesDto {
        val types = safeOf(type, FlowActivityType.values().toList())

        val cont = ActivityContinuation.of(continuation)

        val predicateBuilder = BooleanBuilder(byTypes(types)).and(byCollection(collection))

        if (cont != null) {
            predicateBuilder.and(byContinuation(cont))
        }
        val flow = itemHistoryRepository.findAll(predicateBuilder, *order(sort)).asFlow()

        return flowActivitiesDto(flow, size)
    }

    private fun byCollection(collection: String): BooleanExpression {
        val q = QItemHistory.itemHistory
        return q.activity.`as`(QFlowNftActivity::class.java).contract.eq(collection)
    }

    private fun byContinuation(cont: ActivityContinuation): BooleanExpression {
        val q = QItemHistory.itemHistory
        return q.date.before(cont.beforeDate).and(q.id.loe(cont.beforeId))
    }

    private fun byTypes(types: List<FlowActivityType>): BooleanExpression {
        val qItemHistory = QItemHistory.itemHistory
        return qItemHistory.activity.`as`(QBaseActivity::class.java).type.`in`(types)
    }

    private fun byContractAndTokenPredicate(contract: String, tokenId: Long): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QFlowNftActivity(q.activity.metadata)
        return activity.contract.eq(contract)
            .and(activity.tokenId.eq(tokenId))
    }

    private fun withintDates(qItemHistory: QItemHistory, predicate: BooleanExpression, from: Instant?, to: Instant?): BooleanExpression {
        var pred = predicate
        if(from != null) {
            pred = predicate.and(qItemHistory.date.after(from))
        }
        if(to != null) {
            pred = pred.and(qItemHistory.date.before(to))
        }

        return pred
    }

    private fun transferFromPredicate(users: List<String>, from: Instant?, to: Instant?): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QWithdrawnActivity(q.activity.metadata)
        var predicate = activity.type.eq(FlowActivityType.WITHDRAWN)
            .and(activity.from.`in`(users))

        return withintDates(q, predicate, from, to)
    }

    private fun transferToPredicate(users: List<String>, from: Instant?, to: Instant?): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QDepositActivity(q.activity.metadata)
        val predicate = activity.type.eq(FlowActivityType.DEPOSIT)
            .and(activity.to.`in`(users))

        return withintDates(q, predicate, from, to)
    }

    private fun byOwner(users: List<String>): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QFlowNftActivity(q.activity.metadata)
        return activity.owner.isNotNull.and(activity.owner.`in`(users))
    }

    private fun order(sort: String?): Array<OrderSpecifier<*>> {
        val q = QItemHistory.itemHistory

        return when (sort) {
            null, "LATEST_FIRST" -> arrayOf(
                q.date.desc(),
                q.id.desc()
            )
            "EARLIEST_FIRST" -> arrayOf(
                q.date.asc(),
                q.id.asc()
            )
            else -> throw IllegalArgumentException("Unsupported sort type: $sort")
        }
    }

    private fun answerContinuation(items: List<ItemHistory>): ActivityContinuation? =
        if (items.isEmpty()) null else ActivityContinuation(beforeDate = items.last().date, beforeId = items.last().id)


    private fun sellPredicate(users: List<String>, from: Instant?, to: Instant?): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QFlowNftOrderActivitySell(q.activity.metadata)
        val predicate = activity.type.eq(FlowActivityType.SELL)
            .and(activity.left.maker.`in`(users))
        return withintDates(q, predicate, from, to)
    }

    private fun listPredicate(users: List<String>, from: Instant?, to: Instant?): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QFlowNftOrderActivityList(q.activity.metadata)
        val predicate = activity.type.eq(FlowActivityType.LIST)
            .and(activity.maker.`in`(users))
        return withintDates(q, predicate, from, to)
    }
}

