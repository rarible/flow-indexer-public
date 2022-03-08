package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.repository.filters.CriteriaProduct
import com.rarible.flow.core.repository.filters.DbFilter
import com.rarible.flow.core.repository.filters.ScrollingSort
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.query.*
import java.time.Instant


sealed class ItemHistoryFilter : DbFilter<ItemHistory>, CriteriaProduct<ItemHistoryFilter> {

    enum class Sort : ScrollingSort<ItemHistory> {
        EARLIEST_FIRST {
            override fun springSort(): org.springframework.data.domain.Sort =
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc(ItemHistory::date.name),
                    org.springframework.data.domain.Sort.Order.asc(ItemHistory::id.name)
                )

            override fun scroll(criteria: Criteria, continuation: String?): Criteria =
                Cont.scrollAsc(criteria, continuation, ItemHistory::date, ItemHistory::id)


            override fun nextPage(entity: ItemHistory): String =
                Cont.toString(entity.date, entity.id)
        },

        EARLIEST_LAST {
            override fun springSort(): org.springframework.data.domain.Sort =
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc(ItemHistory::date.name),
                    org.springframework.data.domain.Sort.Order.desc(ItemHistory::id.name)
                )

            override fun scroll(criteria: Criteria, continuation: String?): Criteria =
                Cont.scrollDesc(criteria, continuation, ItemHistory::date, ItemHistory::id)


            override fun nextPage(entity: ItemHistory): String =
                Cont.toString(entity.date, entity.id)
        }
    }

    override fun byCriteria(criteria: Criteria): ItemHistoryFilter {
        return ByCriteria(criteria)
    }

    private data class ByCriteria(val criteria: Criteria) : ItemHistoryFilter() {
        override fun criteria(): Criteria {
            return criteria
        }
    }

    data class ByTypes(val types: Collection<FlowActivityType>) : ItemHistoryFilter() {
        override fun criteria(): Criteria {
            return ItemHistory::activity / BaseActivity::type inValues types
        }
    }

    data class ByCollection(val collection: String) : ItemHistoryFilter() {
        override fun criteria(): Criteria {
            return Criteria("activity.contract").isEqualTo(collection)
        }
    }

    data class ByItem(val contract: String, val tokenId: Long) : ItemHistoryFilter() {
        override fun criteria(): Criteria {
            return Criteria().andOperator(
                Criteria("activity.contract").isEqualTo(contract),
                Criteria("activity.tokenId").isEqualTo(tokenId)
            )
        }
    }

    data class ByUsers(val types: Collection<FlowActivityType>, val users: List<String>) : ItemHistoryFilter() {
        override fun criteria(): Criteria {
            val criterion = types.map { t ->
                userCriteria[t]?.let { it(users) } ?: (ItemHistory::activity / BaseActivity::type isEqualTo t).and("activity.owner").`in`(users)
            }

            return if(criterion.isEmpty()) {
                return Criteria()
            } else {
                Criteria().orOperator(criterion)
            }
        }

        companion object {
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
                    Criteria.where("activity.type").`in`(FlowActivityType.LIST, FlowActivityType.CANCEL_LIST)
                        .and("activity.maker").`in`(u)
                },
                FlowActivityType.CANCEL_LIST to { u: List<String> ->
                    Criteria.where("activity.type").isEqualTo(FlowActivityType.CANCEL_LIST)
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
        }
    }

    data class From(val from: Instant?) : ItemHistoryFilter() {
        override fun criteria(): Criteria {
            return if(from == null) {
                Criteria()
            } else {
                ItemHistory::date gte from
            }
        }
    }

    data class To(val to: Instant?) : ItemHistoryFilter() {
        override fun criteria(): Criteria {
            return if(to == null) {
                Criteria()
            } else {
                ItemHistory::date lt to
            }
        }
    }
}
