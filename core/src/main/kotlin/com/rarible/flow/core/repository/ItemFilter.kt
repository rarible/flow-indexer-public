package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.repository.filters.CriteriaProduct
import com.rarible.flow.core.repository.filters.DbFilter
import com.rarible.flow.core.repository.filters.ScrollingSort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.query.ne
import java.time.Instant
import org.springframework.data.domain.Sort as SpringSort


sealed class ItemFilter(open val sort: Sort = Sort.LAST_UPDATE) : DbFilter<Item>, CriteriaProduct<ItemFilter> {
    enum class Sort: ScrollingSort<Item> {
        LAST_UPDATE {
            override fun springSort(): SpringSort =
                SpringSort.by(
                    SpringSort.Order.desc(Item::updatedAt.name),
                    SpringSort.Order.desc(Item::id.name)
                )

            override fun scroll(criteria: Criteria, continuation: String?): Criteria =
                Cont.scrollDesc(criteria, continuation, Item::updatedAt, Item::id)


            override fun nextPage(entity: Item): String =
                Cont.toString(entity.updatedAt, entity.id)
        }
    }

    override fun byCriteria(criteria: Criteria): ItemFilter {
        return ByCriteria(criteria)
    }

    private data class ByCriteria(val criteria: Criteria) : ItemFilter() {
        override fun criteria(): Criteria {
            return criteria
        }
    }

    data class ByShowDeleted(val showDeleted: Boolean = false) : ItemFilter() {
        override fun criteria(): Criteria {
            return if (showDeleted) {
                Criteria()
            } else {
                Criteria().andOperator(
                    Item::owner exists true,
                    Item::owner ne null
                )
            }
        }
    }

    data class ByLastUpdatedFrom(val from: Instant?) : ItemFilter() {
        override fun criteria(): Criteria {
            return if (from == null) {
                Criteria()
            } else {
                Item::updatedAt gte from
            }
        }
    }

    data class ByLastUpdatedTo(val to: Instant?) : ItemFilter() {
        override fun criteria(): Criteria {
            return if (to == null) {
                Criteria()
            } else {
                Item::updatedAt lt to
            }
        }
    }

    data class All(
        val showDeleted: Boolean = false,
        val lastUpdatedFrom: Instant? = null,
        val lastUpdatedTo: Instant? = null,
    ) : ItemFilter() {
        override fun criteria(): Criteria {
            return (
                ByShowDeleted(showDeleted) *
                    ByLastUpdatedFrom(lastUpdatedFrom) *
                    ByLastUpdatedTo(lastUpdatedTo)
            ).criteria()
        }
    }

    data class ByOwner(val owner: FlowAddress) : ItemFilter() {
        override fun criteria(): Criteria {
            return Item::owner isEqualTo owner
        }
    }

    data class ByCreator(val creator: FlowAddress) : ItemFilter() {
        override fun criteria(): Criteria {
            return (Item::creator isEqualTo creator).andOperator(
                All(false).criteria()
            )
        }
    }

    data class ByCollection(val collectionId: String) : ItemFilter() {
        override fun criteria(): Criteria {
            return (Item::collection isEqualTo collectionId).andOperator(
                All(false).criteria()
            )
        }
    }

}
