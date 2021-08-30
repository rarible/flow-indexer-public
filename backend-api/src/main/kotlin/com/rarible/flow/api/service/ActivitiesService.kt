package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.protocol.dto.FlowActivitiesDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service

@FlowPreview
@Service
class ActivitiesService(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository
) {
    suspend fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?
    ): FlowActivitiesDto {
        var types = type.map { FlowActivityType.valueOf(it) }

        if (types.isEmpty()) {
            types = FlowActivityType.values().toList()
        }

        val cont = ActivityContinuation.of(continuation)

        val order = defaultOrder()
        var predicate = byTypes(types).and(byContractAndTokenPredicate(contract, tokenId))

        if (cont != null) {
            predicate = predicate.and(byContinuation(cont))
        }

        val flow = itemHistoryRepository.findAll(predicate, *order).asFlow()
        return flowActivitiesDto(flow, size)
    }

    suspend fun getNftOrderActivitiesByUser(
        type: List<String>,
        user: List<String>,
        continuation: String?,
        size: Int?
    ): FlowActivitiesDto {
        val haveTransferTo = type.isEmpty() || type.contains("TRANSFER_TO")
        val haveTransferFrom = type.isEmpty() || type.contains("TRANSFER_FROM")

        val types = if (type.isEmpty()) {
            FlowActivityType.values().toList()
        } else {
            type.filter { "TRANSFER_TO" != it && "TRANSFER_FROM" != it }.map { FlowActivityType.valueOf(it) }
        }
        val users = user.map { FlowAddress(it) }
        val cont = ActivityContinuation.of(continuation)

        val order = defaultOrder()
        val predicate = BooleanBuilder(byTypes(types))
        if (cont != null) {
            predicate.and(byContinuation(cont))
        }

        val activities: Flow<ItemHistory> = itemHistoryRepository.findAll(predicate.and(byOwner(users)), *order).asFlow()
        val transferFromActivities: Flow<ItemHistory> = if (haveTransferFrom) {
            itemHistoryRepository.findAll(transferFromPredicate(users), *order).asFlow()
        } else emptyFlow()


        val transferToActivities: Flow<ItemHistory> = if (haveTransferTo) {
            itemHistoryRepository.findAll(transferToPredicate(users)).asFlow()
        } else emptyFlow()

        return flowActivitiesDto(flowOf(activities, transferFromActivities, transferToActivities).flattenConcat(), size)
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
            items = items.map { h -> h.activity.toDto(h.id, h.date) },
            total = items.size,
            continuation = "${answerContinuation(items)}"
        )
    }

    suspend fun getNftOrderAllActivities(type: List<String>, continuation: String?, size: Int?): FlowActivitiesDto {
        val types = if (type.isEmpty()) {
            FlowActivityType.values().toList()
        } else {
            type.map { FlowActivityType.valueOf(it) }.toList()
        }

        val cont = ActivityContinuation.of(continuation)
        val order = defaultOrder()
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
        size: Int?
    ): FlowActivitiesDto {
        val types =
            if (type.isEmpty()) FlowActivityType.values().toList() else type.map { FlowActivityType.valueOf(it) }

        val cont = ActivityContinuation.of(continuation)

        val predicateBuilder = BooleanBuilder(byTypes(types)).and(byCollection(collection))

        if (cont != null) {
            predicateBuilder.and(byContinuation(cont))
        }
        val flow = itemHistoryRepository.findAll(predicateBuilder, *defaultOrder()).asFlow()

        return flowActivitiesDto(flow, size)
    }

    private fun byCollection(collection: String): BooleanExpression {
        val q = QItemHistory.itemHistory
        return q.activity.`as`(QFlowNftActivity::class.java).collection.eq(collection)
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
        val qItemHistory = QItemHistory.itemHistory
        return qItemHistory.activity.`as`(QFlowNftActivity::class.java).contract.eq(contract)
            .and(qItemHistory.activity.`as`(QBaseActivity::class.java).tokenId.eq(tokenId))
    }

    private fun transferFromPredicate(users: List<FlowAddress>): BooleanExpression {
        val q = QItemHistory.itemHistory
        return q.activity.`as`(QBaseActivity::class.java).type.eq(FlowActivityType.TRANSFER)
            .and(q.activity.`as`(QTransferActivity::class.java).from.`in`(*users.toTypedArray()))
    }

    private fun transferToPredicate(users: List<FlowAddress>): BooleanBuilder {
        val q = QItemHistory.itemHistory
        return BooleanBuilder(q.activity.`as`(QBaseActivity::class.java).type.eq(FlowActivityType.TRANSFER))
            .and(q.activity.`as`(QTransferActivity::class.java).owner.`in`(users))
    }

    private fun byOwner(users: List<FlowAddress>): BooleanBuilder {
        val q = QItemHistory.itemHistory
        return BooleanBuilder(q.activity.`as`(QFlowNftActivity::class.java).owner.isNotNull.and(q.activity.`as`(QFlowNftActivity::class.java).owner.`in`(users)))
    }

    private fun defaultOrder(): Array<OrderSpecifier<*>> {
        val q = QItemHistory.itemHistory
        return arrayOf(
            q.date.desc(),
            q.id.desc()
        )
    }

    private fun answerContinuation(items: List<ItemHistory>): ActivityContinuation? =
        if (items.isEmpty()) null else ActivityContinuation(beforeDate = items.last().date, beforeId = items.last().id)
}

