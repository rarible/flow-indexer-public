package com.rarible.flow.api.service

import com.rarible.flow.core.converter.ItemHistoryToDtoConverter
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.core.repository.filters.ScrollingSort
import com.rarible.flow.enum.safeOf
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.Instant

@FlowPreview
@Service
class ActivitiesService(
    private val mongoTemplate: ReactiveMongoTemplate,
) {

    companion object {

        private val queryTypes = setOf(
            FlowActivityType.TRANSFER,
            FlowActivityType.MINT,
            FlowActivityType.BURN,
            FlowActivityType.SELL,
            FlowActivityType.LIST,
            FlowActivityType.CANCEL_LIST,
            FlowActivityType.BID,
            FlowActivityType.CANCEL_BID,
        )

        private val userCriteria = mapOf(
            FlowActivityType.TRANSFER_FROM to { u: List<String> ->
                Criteria.where("activity.type").isEqualTo(FlowActivityType.TRANSFER.name)
                    .and("activity.from").`in`(u)
            },
            FlowActivityType.TRANSFER_TO to { u: List<String> ->
                Criteria.where("activity.type").isEqualTo(FlowActivityType.TRANSFER.name)
                    .and("activity.to").`in`(u)
            },
            FlowActivityType.LIST to { u: List<String> ->
                Criteria.where("activity.type").isEqualTo(FlowActivityType.LIST.name)
                    .and("activity.maker").`in`(u)
            },
            FlowActivityType.CANCEL_BID to { u: List<String> ->
                Criteria.where("activity.type").isEqualTo(FlowActivityType.CANCEL_BID.name)
                    .and("activity.maker").`in`(u)
            },
            FlowActivityType.MAKE_BID to { u: List<String> ->
                Criteria.where("activity.type")
                    .`in`(listOf(FlowActivityType.BID.name, FlowActivityType.CANCEL_BID.name))
                    .and("activity.maker").`in`(u)
            },
            FlowActivityType.GET_BID to { u: List<String> ->
                Criteria.where("activity.type").isEqualTo(FlowActivityType.SELL.name)
                    .and("activity.left.asset.tokenId").exists(true)
                    .and("activity.right.maker").`in`(u)
            },
            FlowActivityType.BUY to { u: List<String> ->
                Criteria.where("activity.type").isEqualTo(FlowActivityType.SELL.name)
                    .and("activity.right.maker").`in`(u)
            },
            FlowActivityType.SELL to { u: List<String> ->
                Criteria.where("activity.type").isEqualTo(FlowActivityType.SELL.name)
                    .and("activity.left.maker").`in`(u)
            }
        )

        private val emptyActivities = FlowActivitiesDto(items = emptyList(), total = 0)
    }

    suspend fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val types = if (type.isEmpty()) queryTypes else safeOf(type)
        if (types.isEmpty()) return emptyActivities

        val criteria = defaultCriteria(types)
            .and("activity.contract").isEqualTo(contract)
            .and("activity.tokenId").isEqualTo(tokenId)

        return getActivities(criteria, continuation, size, sort)
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
        val types = if (type.isEmpty()) queryTypes + userCriteria.keys else safeOf(type)
        if (types.isEmpty()) return emptyActivities

        val arrayOfCriteria = types.map { t ->
            userCriteria[t]?.let { it(user) } ?: Criteria.where("activity.type").isEqualTo(t.name).and("activity.owner")
                .`in`(user)
        }.toTypedArray()

        val criteria = Criteria()
            .orOperator(*arrayOfCriteria)
            .andDates(from, to)

        return getActivities(criteria, continuation, size, sort)
    }

    suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val types = if (type.isEmpty()) queryTypes else safeOf(type)
        if (types.isEmpty()) return emptyActivities

        val criteria = defaultCriteria(types)

        return getActivities(criteria, continuation, size, sort)
    }

    suspend fun getNftOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        val types = if (type.isEmpty()) queryTypes else safeOf(type)
        if (types.isEmpty()) return emptyActivities

        val criteria = defaultCriteria(types)
            .and("activity.contract").isEqualTo(collection)

        return getActivities(criteria, continuation, size, sort)
    }

    suspend fun getActivitiesByIds(ids: List<String>): FlowActivitiesDto {
        return getActivities(
            ItemHistory::id inValues ids,
            null,
            null,
            "EARLIEST_FIRST"
        )
    }

    suspend fun getNftOrderActivitiesByItemAndOwner(
        type: List<String>,
        contract: String,
        tokenId: Long,
        owner: String,
        continuation: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto? {
        val types = if (type.isEmpty()) queryTypes else safeOf(type)
        if (types.isEmpty()) return emptyActivities

        val criteria = defaultCriteria(types).andOperator(
            Criteria("activity.contract").isEqualTo(contract),
            Criteria("activity.tokenId").isEqualTo(tokenId),
            Criteria().orOperator(
                Criteria("activity.owner").isEqualTo(owner),
                Criteria("activity.to").isEqualTo(owner),
            )
        )

        return getActivities(criteria, continuation, size, sort)
    }

    private suspend fun getActivities(
        criteria: Criteria,
        inCont: String?,
        size: Int?,
        sort: String,
    ): FlowActivitiesDto {
        addContinuation(ActivityContinuation.of(inCont), criteria, sort)
        val limit = ScrollingSort.Companion.pageSize(size)
        val query = Query()
            .addCriteria(criteria)
            .limit(limit)
            .with(defaultSort(sort))
        val items = mongoTemplate
            .find(query, ItemHistory::class.java).asFlow()
            .map { ItemHistoryToDtoConverter.convert(it) }
            .toList()
        val outCont = answerContinuation(items, limit)

        return FlowActivitiesDto(
            items = items,
            total = items.size,
            continuation = outCont.toString()
        )
    }

    private fun Criteria.andDates(from: Instant?, to: Instant?) = apply {
        val c = listOfNotNull(
            from?.let { Criteria.where("date").gte(it) },
            to?.let { Criteria.where("date").gte(it) }
        )
        if (c.isNotEmpty()) andOperator(*c.toTypedArray())
    }

    private fun addContinuation(cont: ActivityContinuation?, criteria: Criteria, sort: String) {
        if (cont != null) {
            when (sort) {
                "EARLIEST_FIRST" ->
                    criteria.andOperator(
                        Criteria().orOperator(
                            where(ItemHistory::date).gt(cont.beforeDate),
                            where(ItemHistory::date).isEqualTo(cont.beforeDate).and(ItemHistory::id).gt(cont.beforeId)
                        )
                    )
                else ->
                    criteria.andOperator(
                       Criteria().orOperator(
                           where(ItemHistory::date).lt(cont.beforeDate),
                           where(ItemHistory::date).isEqualTo(cont.beforeDate).and(ItemHistory::id).lt(cont.beforeId)
                       )
                    )
            }
        }
    }

    private fun defaultCriteria(types: Collection<FlowActivityType>) =
        Criteria.where("activity.type").`in`(types.map(FlowActivityType::name))

    private fun defaultSort(sort: String): Sort = when (sort) {
        "EARLIEST_FIRST" -> Sort.by(Sort.Direction.ASC, "date", "_id")
        else -> Sort.by(Sort.Direction.DESC, "date", "_id")
    }

    private fun answerContinuation(items: List<FlowActivityDto>, limit: Int): ActivityContinuation? =
        if (items.size < limit) null
        else ActivityContinuation(beforeDate = items.last().date, beforeId = items.last().id)

}
