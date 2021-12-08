package com.rarible.flow.api.service

import com.rarible.flow.core.converter.ItemHistoryToDtoConverter
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.core.repository.filters.ScrollingSort
import com.rarible.flow.enum.safeOf
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowTransferDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.time.Instant

@FlowPreview
@Service
class ActivitiesService(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    suspend fun getNftOrderActivitiesByItem(
        types: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val queryTypes = if (types.isEmpty()) FlowActivityType.values().toMutableList() else safeOf<FlowActivityType>(
            types
        ).toMutableList()
        if (queryTypes.isEmpty()) {
            return FlowActivitiesDto(items = emptyList(), total = 0)
        }
        val cont = ActivityContinuation.of(continuation)
        val query = defaultQuery(size).with(defaultSort(sort))
        val criteria = Criteria.where("activity.type").`in`(fixTypes(queryTypes).toSet())
            .and("activity.contract").isEqualTo(contract)
            .and("activity.tokenId").isEqualTo(tokenId)
        addContinuation(cont, criteria, sort)
        val flow = mongoTemplate.find(query.addCriteria(criteria), ItemHistory::class.java).asFlow()
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
        var queryTypes = if (type.isEmpty()) FlowActivityType.values().toMutableList() else safeOf<FlowActivityType>(
            type
        ).toMutableList()
        if (queryTypes.isEmpty()) {
            return FlowActivitiesDto(items = emptyList(), total = 0)
        }
        val haveTransferTo = type.isEmpty() || queryTypes.contains(FlowActivityType.TRANSFER_TO)
        val haveTransferFrom = type.isEmpty() || queryTypes.contains(FlowActivityType.TRANSFER_FROM)
        val haveBuy = type.isEmpty() || queryTypes.contains(FlowActivityType.BUY)

        queryTypes = fixTypes(queryTypes).toMutableList()

        val cont = ActivityContinuation.of(continuation)
        val transferFromActivities: Flow<ItemHistory> = if (haveTransferFrom) {
            getTransferFromActivities(user, cont, sort, from, to, size)
        } else emptyFlow()
        queryTypes.remove(FlowActivityType.TRANSFER_FROM)

        val transferToActivities: Flow<ItemHistory> = if (haveTransferTo) {
            getTransferToActivities(user, cont, sort, from, to, size)
        } else emptyFlow()
        queryTypes.remove(FlowActivityType.TRANSFER_TO)

        val listActivities: Flow<ItemHistory> = if (queryTypes.contains(FlowActivityType.LIST)) {
            val query = defaultQuery(size).with(defaultSort(sort))
            val criteria = Criteria.where("activity.type").isEqualTo(FlowActivityType.LIST)
                .and("activity.maker").`in`(user)
            addContinuation(cont, criteria, sort)
            addDates(criteria, from, to)
            mongoTemplate.find(query.addCriteria(criteria), ItemHistory::class.java).asFlow()
        } else emptyFlow()
        queryTypes.remove(FlowActivityType.LIST)

        val cancelListActivities: Flow<ItemHistory> = if (queryTypes.contains(FlowActivityType.CANCEL_LIST)) {
            val query = defaultQuery(size).with(defaultSort(sort))
            val criteria = Criteria.where("activity.type").isEqualTo(FlowActivityType.CANCEL_LIST)
                .and("activity.maker").`in`(user)
            addContinuation(cont, criteria, sort)
            addDates(criteria, from, to)
            mongoTemplate.find(query.addCriteria(criteria), ItemHistory::class.java).asFlow()
        } else emptyFlow()
        queryTypes.remove(FlowActivityType.CANCEL_LIST)

        val buyActivities: Flow<ItemHistory> = if (haveBuy) {
            val query = defaultQuery(size).with(defaultSort(sort))
            val criteria = Criteria.where("activity.type").isEqualTo(FlowActivityType.SELL)
                .and("activity.right.maker").`in`(user)
            addContinuation(cont, criteria, sort)
            addDates(criteria, from, to)
            mongoTemplate.find(query.addCriteria(criteria), ItemHistory::class.java).asFlow()
        } else emptyFlow()

        val sellActivities: Flow<ItemHistory> = if (queryTypes.contains(FlowActivityType.SELL)) {
            val query = defaultQuery(size).with(defaultSort(sort))
            val criteria = Criteria.where("activity.type").isEqualTo(FlowActivityType.SELL)
                .and("activity.left.maker").`in`(user)
            addContinuation(cont, criteria, sort)
            addDates(criteria, from, to)
            mongoTemplate.find(query.addCriteria(criteria), ItemHistory::class.java).asFlow()
        } else emptyFlow()
        queryTypes.remove(FlowActivityType.SELL)

        val burnActivities = if (queryTypes.contains(FlowActivityType.BURN)) {
            getBurnActivities(user, cont, sort, from, to, size)
        } else emptyFlow()
        queryTypes.remove(FlowActivityType.BURN)

        if (haveTransferFrom || haveTransferTo) {
            queryTypes.remove(FlowActivityType.TRANSFER)
        }

        val activities: Flow<ItemHistory> = if (queryTypes.isEmpty()) emptyFlow() else {
            val query = defaultQuery(size).with(defaultSort(sort))
            val criteria = Criteria.where("activity.type").`in`(queryTypes.toSet())
                .and("activity.owner").`in`(user)
            addContinuation(cont, criteria, sort)
            addDates(criteria, from, to)
            mongoTemplate.find(query.addCriteria(criteria), ItemHistory::class.java).asFlow()
        }

        return flowActivitiesDto(
            flowOf(
                activities,
                transferFromActivities,
                transferToActivities,
                listActivities,
                cancelListActivities,
                sellActivities,
                buyActivities,
                burnActivities,
            ).flattenConcat(),
            size, sort
        )
    }

    private fun getBurnActivities(
        user: List<String>,
        cont: ActivityContinuation?,
        sort: String,
        from: Instant?,
        to: Instant?,
        size: Int?
    ): Flow<ItemHistory> {
        val query = defaultQuery(size).with(defaultSort(sort))
        val criteria = Criteria.where("activity.type").isEqualTo(FlowActivityType.BURN)
        addContinuation(cont, criteria, sort)
        addDates(criteria, from, to)
        return mongoTemplate.find(query.addCriteria(criteria), ItemHistory::class.java).asFlow().flatMapConcat {
            val c = Criteria.where("activity.type").isEqualTo(FlowActivityType.WITHDRAWN)
                .and("activity.from").`in`(user)
                .and("activity.tokenId").isEqualTo((it.activity as NFTActivity).tokenId)
                .and("activity.contract").isEqualTo((it.activity as NFTActivity).contract)
                .and("log.transactionHash").isEqualTo(it.log.transactionHash)
                .and("log.eventIndex").lt(it.log.eventIndex)
            addContinuation(cont, c, sort)
            flowOf(
                it,
                mongoTemplate.find(
                    defaultQuery(size).addCriteria(c).with(Sort.by(Sort.Direction.DESC, "date", "log.eventIndex")),
                    ItemHistory::class.java
                ).awaitFirstOrNull()
            )
        }.filterNotNull()
    }

    private fun addDates(criteria: Criteria, from: Instant?, to: Instant?) {
        if (from != null) {
            criteria.and("date").gte(from)
        }
        if (to != null) {
            criteria.and("date").lte(to)
        }
    }

    private fun getTransferToActivities(
        user: List<String>,
        cont: ActivityContinuation?,
        sort: String,
        from: Instant?,
        to: Instant?,
        size: Int?
    ): Flow<ItemHistory> {
        val query = defaultQuery(size).with(defaultSort(sort))
        val criteria =
            Criteria.where("activity.to").`in`(user).and("activity.type").isEqualTo(FlowActivityType.DEPOSIT.name)
        addContinuation(cont, criteria, sort)
        addDates(criteria, from, to)
        return mongoTemplate.find(query.addCriteria(criteria), ItemHistory::class.java).asFlow().flatMapConcat {
            val c = Criteria.where("activity.type").isEqualTo(FlowActivityType.WITHDRAWN)
                .and("activity.tokenId").isEqualTo((it.activity as NFTActivity).tokenId)
                .and("activity.contract").isEqualTo((it.activity as NFTActivity).contract)
                .and("log.transactionHash").isEqualTo(it.log.transactionHash)
                .and("log.eventIndex").lt(it.log.eventIndex)
            addContinuation(cont, c, sort)
            flowOf(
                it,
                mongoTemplate.find(
                    defaultQuery(size).addCriteria(c).with(Sort.by(Sort.Direction.DESC, "date", "log.eventIndex")),
                    ItemHistory::class.java
                ).awaitFirstOrNull()
            ).filterNotNull()
        }
    }

    private fun getTransferFromActivities(
        user: List<String>,
        cont: ActivityContinuation?,
        sort: String,
        from: Instant?,
        to: Instant?,
        size: Int?
    ): Flow<ItemHistory> {
        val query = defaultQuery(size)
        val criteria =
            Criteria.where("activity.from").`in`(user).and("activity.type").isEqualTo(FlowActivityType.WITHDRAWN.name)
        addContinuation(cont, criteria, sort)
        addDates(criteria, from, to)
        return mongoTemplate.find(query.addCriteria(criteria).with(defaultSort(sort)), ItemHistory::class.java).asFlow()
            .flatMapConcat {
                val c =
                    Criteria.where("activity.type").isEqualTo(FlowActivityType.DEPOSIT)
                        .and("activity.tokenId").isEqualTo((it.activity as NFTActivity).tokenId)
                        .and("activity.contract").isEqualTo((it.activity as NFTActivity).contract)
                        .and("log.transactionHash").isEqualTo(it.log.transactionHash)
                        .and("log.eventIndex").gt(it.log.eventIndex)
                addContinuation(cont, c, sort)
                flowOf(
                    it,
                    mongoTemplate.find(
                        defaultQuery(size).addCriteria(c).with(Sort.by(Sort.Direction.DESC, "date", "log.eventIndex")),
                        ItemHistory::class.java
                    ).awaitFirstOrNull()
                ).filterNotNull()
            }
    }

    private fun addContinuation(cont: ActivityContinuation?, criteria: Criteria, sort: String) {
        if (cont != null) {
            when (sort) {
                "EARLIEST_FIRST" -> criteria.and("date").gte(cont.beforeDate)
                else -> criteria.and("date").lte(cont.beforeDate)
            }
            criteria.and("id").ne(cont.beforeId)
        }
    }

    private fun defaultQuery(limit: Int?): Query = Query().limit(
        ScrollingSort.Companion.pageSize(limit) * 3
    )

    private fun defaultSort(sort: String): Sort = when (sort) {
        "EARLIEST_FIRST" -> Sort.by(Sort.Direction.ASC, "date", "log.transactionHash", "log.eventIndex")
        else -> Sort.by(Sort.Direction.DESC, "date", "log.transactionHash", "log.eventIndex")
    }

    suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val types = if (type.isEmpty()) FlowActivityType.values()
            .toMutableList() else safeOf<FlowActivityType>(type).toMutableList()
        if (types.isEmpty()) {
            return FlowActivitiesDto(items = emptyList(), total = 0)
        }

        val cont = ActivityContinuation.of(continuation)
        val query = defaultQuery(size).with(defaultSort(sort))
        val criteria = Criteria.where("activity.type").`in`(fixTypes(types).toSet())
        addContinuation(cont, criteria, sort)
        query.addCriteria(criteria)
        return flowActivitiesDto(mongoTemplate.find(query, ItemHistory::class.java).asFlow(), size, sort)
    }

    suspend fun getNftOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val queryTypes = if (type.isEmpty()) FlowActivityType.values().toList() else safeOf(type)
        if (queryTypes.isEmpty()) {
            return FlowActivitiesDto(items = emptyList(), total = 0)
        }

        val cont = ActivityContinuation.of(continuation)
        val criteria = Criteria.where("activity.type").`in`(fixTypes(queryTypes).toSet())
            .and("activity.contract").isEqualTo(collection)
        addContinuation(cont, criteria, sort)
        val query = defaultQuery(size).with(defaultSort(sort))
        val flow = mongoTemplate.find(query.addCriteria(criteria), ItemHistory::class.java).asFlow()
        return flowActivitiesDto(flow, size, sort)
    }

    private suspend fun flowActivitiesDto(
        flow: Flow<ItemHistory>,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val items = flow.toList()
        val limit = ScrollingSort.Companion.pageSize(size)
        val dto = convertToDto(items, sort).take(limit)
        val continuation =
            if (items.size <= limit) null else if (items.size >= dto.size && dto.size < limit) null else answerContinuation(
                dto
            )?.toString()
        return FlowActivitiesDto(
            items = dto,
            total = dto.size,
            continuation = continuation
        )
    }

    private fun fixTypes(types: List<FlowActivityType>) =
        if (types.contains(FlowActivityType.TRANSFER)) {
            types - FlowActivityType.TRANSFER + FlowActivityType.DEPOSIT + FlowActivityType.WITHDRAWN
        } else if (types.contains(FlowActivityType.BURN)) {
            types + FlowActivityType.WITHDRAWN
        } else if (types.contains(FlowActivityType.SELL)) {
            types + FlowActivityType.CANCEL_LIST
        } else {
            types
        }

    private fun answerContinuation(items: List<FlowActivityDto>): ActivityContinuation? =
        if (items.isEmpty()) null else ActivityContinuation(beforeDate = items.last().date, beforeId = items.last().id)

    private fun convertToDto(
        history: List<ItemHistory>,
        sort: String,
    ): List<FlowActivityDto> {
        val result = mutableListOf<FlowActivityDto>()
        for (i in history.indices) {
            val h = history[i]
            if (h.activity is DepositActivity || h.activity is BurnActivity) {
                continue
            }
            if (h.activity is WithdrawnActivity) {
                val wa = h.activity as WithdrawnActivity
                if (wa.from.isNullOrEmpty()) {
                    continue
                }
                val d = findActivity(history, i, FlowActivityType.DEPOSIT)
                if (d != null) {
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
                } else {
                    val b = findActivity(history, i, FlowActivityType.BURN)
                    if (b != null) {
                        val ba = b.activity as BurnActivity
                        result.add(
                            ItemHistoryToDtoConverter.convert(
                                h.copy(activity = ba.copy(owner = wa.from))
                            )!!
                        )
                    }
                }
                continue
            }
            ItemHistoryToDtoConverter.convert(h)?.let {
                result.add(it)
            }
        }

        val dtoList = result.sortedWith(compareBy(FlowActivityDto::date)
            .thenBy { it.id.substringBefore(".") }
            .thenBy { it.id.substringAfter(".").padStart(4, '0') }
        )

        return if (sort == "EARLIEST_FIRST") dtoList else dtoList.reversed()
    }


    fun findActivity(history: List<ItemHistory>, index: Int, type: FlowActivityType): ItemHistory? {
        val h = history[index]
        val a = h.activity as NFTActivity
        val hash = h.log.transactionHash

        fun isFit(activity: NFTActivity) =
            activity.type == type && activity.contract == a.contract && activity.tokenId == a.tokenId

        tailrec fun helper(lag: Int = 1, checkNext: Boolean = true): ItemHistory? {
            val i = index + lag
            val isUseful = (i >= 0 && i <= history.lastIndex) && history[i].log.transactionHash == hash
            if (isUseful) {
                val item = history[i]
                if (isFit(item.activity as NFTActivity)) return item
            }
            return if (isUseful || checkNext) helper(if (lag > 0) -lag else -lag + 1, isUseful) else null
        }

        return helper()
    }
}
