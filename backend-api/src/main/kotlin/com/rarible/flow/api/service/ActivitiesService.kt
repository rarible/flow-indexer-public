package com.rarible.flow.api.service

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.rarible.blockchain.scanner.flow.model.QFlowLog
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.enum.safeOf
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowTransferDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.time.Instant

@FlowPreview
@Service
class ActivitiesService(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
) {
    suspend fun getNftOrderActivitiesByItem(
        types: List<FlowActivityType>,
        contract: String,
        tokenId: Long,
        continuation: ActivityContinuation?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val order = order(sort)
        var predicate = byTypes(types).and(byContractAndTokenPredicate(contract, tokenId))

        if (continuation != null) {
            predicate = predicate.and(byContinuation(continuation))
        }

        val flow = itemHistoryRepository.findAll(predicate, *order).asFlow()
        return flowActivitiesDto(flow, size, sort)
    }

    suspend fun getNftOrderActivitiesByUser(
        type: List<String>,
        user: List<String>,
        continuation: String?,
        from: Instant?,
        to: Instant?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val haveTransferTo = type.isEmpty() || type.contains("TRANSFER_TO")
        val haveTransferFrom = type.isEmpty() || type.contains("TRANSFER_FROM")
        val haveBuy = type.contains("BUY")
        val skipTypes = mutableListOf<FlowActivityType>()

        val types = if (type.isEmpty()) {
            FlowActivityType.values().toMutableList()
        } else {
            safeOf<FlowActivityType>(
                type.filter { "TRANSFER_TO" != it && "TRANSFER_FROM" != it && "BUY" != it }
            ).toMutableList()
        }
        val cont = ActivityContinuation.of(continuation)

        if (FlowActivityType.BURN in types && FlowActivityType.WITHDRAWN !in types) {
            types.add(FlowActivityType.WITHDRAWN)
            skipTypes.add(FlowActivityType.WITHDRAWN)
        }

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

        val buyActivities: Flow<ItemHistory> = if (haveBuy) {
            itemHistoryRepository.findAll(buyPredicate(user, from, to), *order).asFlow()
        } else emptyFlow()

        val sellActivities: Flow<ItemHistory> = if (types.contains(FlowActivityType.SELL)) {
            itemHistoryRepository.findAll(sellPredicate(user, from, to), *order).asFlow()
        } else emptyFlow()
        types.remove(FlowActivityType.SELL)

        val burnActivities = if (types.contains(FlowActivityType.BURN)) {
            itemHistoryRepository.findAll(burnPredicate(from, to), *order).asFlow()
        } else emptyFlow()
        types.remove(FlowActivityType.BURN)

        if (haveTransferFrom || haveTransferTo) {
            types.remove(FlowActivityType.TRANSFER)
        }

        val activities: Flow<ItemHistory> = if (types.isEmpty()) emptyFlow() else itemHistoryRepository.findAll(
            predicate.and(byTypes(types)).and(byOwner(user, from, to)), *order
        ).asFlow()

        return flowActivitiesDto(
            flowOf(
                activities,
                transferFromActivities,
                transferToActivities,
                listActivities,
                sellActivities,
                buyActivities,
                burnActivities,
            ).flattenConcat(),
            size, sort, skipTypes,
        )
    }

    private suspend fun flowActivitiesDto(
        flow: Flow<ItemHistory>,
        size: Int?,
        sort: String,
        skipTypes: MutableList<FlowActivityType> = mutableListOf(),
    ): FlowActivitiesDto {
        val items = flow.toList()
        var dto = convertToDto(items, sort, skipTypes)
        if (size != null) {
            dto = dto.take(size)
        }
        return FlowActivitiesDto(
            items = dto,
            total = dto.size,
            continuation = "${answerContinuation(items)}"
        )
    }

    suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val types = safeOf(type, FlowActivityType.values().toList())

        val cont = ActivityContinuation.of(continuation)
        val order = order(sort)
        val predicate = byTypes(types)

        if (cont != null) {
            predicate.and(byContinuation(cont))
        }
        return flowActivitiesDto(itemHistoryRepository.findAll(predicate, *order).asFlow(), size, sort)
    }

    suspend fun getNftOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val types = safeOf(type, FlowActivityType.values().toList())

        val cont = ActivityContinuation.of(continuation)

        val predicateBuilder = BooleanBuilder(byTypes(types)).and(byCollection(collection))

        if (cont != null) {
            predicateBuilder.and(byContinuation(cont))
        }
        val flow = itemHistoryRepository.findAll(predicateBuilder, *order(sort)).asFlow()

        return flowActivitiesDto(flow, size, sort)
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
        val fixed = if (types.contains(FlowActivityType.TRANSFER)) {
            val z = types.filter { it != FlowActivityType.TRANSFER }.toMutableList()
            z.addAll(setOf(FlowActivityType.DEPOSIT, FlowActivityType.WITHDRAWN))
            z.toList()
        } else {
            types
        }
        return qItemHistory.activity.`as`(QBaseActivity::class.java).type.`in`(fixed)
    }

    private fun byContractAndTokenPredicate(contract: String, tokenId: Long): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QFlowNftActivity(q.activity.metadata)
        return activity.contract.eq(contract)
            .and(activity.tokenId.eq(tokenId))
    }

    private fun withintDates(
        qItemHistory: QItemHistory,
        predicate: BooleanExpression,
        from: Instant?,
        to: Instant?,
    ): BooleanExpression {
        var pred = predicate
        if (from != null) {
            pred = predicate.and(qItemHistory.date.after(from))
        }
        if (to != null) {
            pred = pred.and(qItemHistory.date.before(to))
        }

        return pred
    }

    private fun transferFromPredicate(users: List<String>, from: Instant?, to: Instant?): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QWithdrawnActivity(q.activity.metadata)
        val predicate = activity.type.eq(FlowActivityType.WITHDRAWN)
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

    private fun byOwner(users: List<String>, from: Instant?, to: Instant?): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QFlowNftActivity(q.activity.metadata)
        val predicate = activity.owner.isNotNull.and(activity.owner.`in`(users))
        return withintDates(q, predicate, from, to)
    }

    private fun order(sort: String?): Array<OrderSpecifier<*>> {
        val q = QItemHistory.itemHistory
        val l = QFlowLog(q.log.metadata)
        return when (sort) {
            null, "LATEST_FIRST" -> arrayOf(
                q.date.desc(),
                l.eventIndex.desc()
            )
            "EARLIEST_FIRST" -> arrayOf(
                q.date.asc(),
                l.eventIndex.asc()
            )
            else -> throw IllegalArgumentException("Unsupported sort type: $sort")
        }
    }

    private fun answerContinuation(items: List<ItemHistory>): ActivityContinuation? =
        if (items.isEmpty()) null else ActivityContinuation(beforeDate = items.last().date, beforeId = items.last().id)

    private fun burnPredicate(from: Instant?, to: Instant?): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QFlowNftOrderActivitySell(q.activity.metadata)
        val predicate = activity.type.eq(FlowActivityType.BURN)
        return withintDates(q, predicate, from, to)
    }

    private fun buyPredicate(users: List<String>, from: Instant?, to: Instant?): BooleanExpression {
        val q = QItemHistory.itemHistory
        val activity = QFlowNftOrderActivitySell(q.activity.metadata)
        val predicate = activity.type.eq(FlowActivityType.SELL)
            .and(activity.right.maker.`in`(users))
        return withintDates(q, predicate, from, to)
    }

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

    private fun convertToDto(
        history: List<ItemHistory>,
        sort: String,
        skipTypes: MutableList<FlowActivityType>,
    ): List<FlowActivityDto> {
        val result = mutableListOf<FlowActivityDto>()
        val sorted = history.sortedWith(compareBy(ItemHistory::date).thenBy { it.log.eventIndex })
        for (i in sorted.indices) {
            var skip = false
            val h = sorted[i]
            if (h.activity is DepositActivity) {
                continue
            }
            if (h.activity is WithdrawnActivity) {
                val wa = h.activity as WithdrawnActivity
                if (i == sorted.lastIndex) {
                    continue
                }
                val d = sorted[i + 1]
                if (d.activity is DepositActivity) {
                    val da = d.activity as DepositActivity
                    result.add(
                        FlowTransferDto(
                            id = d.id,
                            from = wa.from.orEmpty(),
                            owner = da.to.orEmpty(),
                            contract = da.contract,
                            tokenId = da.tokenId.toBigInteger(),
                            value = BigInteger.ONE,
                            transactionHash = d.log.transactionHash,
                            blockHash = d.log.blockHash,
                            blockNumber = d.log.blockHeight,
                            logIndex = d.log.eventIndex,
                            date = da.timestamp
                        )
                    )
                }
                continue
            }
            if (h.activity is BurnActivity) {
                skip = true
                val burnActivity = h.activity as BurnActivity
                val txId = h.log.transactionHash
                var j = i
                while (j > 0 && sorted[j].log.transactionHash == txId) {
                    val a = sorted[j].activity
                    if (a is WithdrawnActivity && a.tokenId == burnActivity.tokenId && a.from != null) {
                        result.add(burnActivity.copy(owner = a.from).toDto(sorted[i]))
                        break
                    }
                    j--
                }
            }
            if (!skip && !(FlowActivityType.WITHDRAWN in skipTypes && sorted[i].activity is WithdrawnActivity))
                result.add(sorted[i].activity.toDto(sorted[i]))
        }
        if (sort == "LATEST_FIRST") {
            result.reverse()
        }
        return result.toList()
    }
}
