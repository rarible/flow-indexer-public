package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import org.springframework.data.mongodb.core.query.*
import java.time.Instant
import java.util.*


sealed class ItemFilter(open val sort: Sort = Sort.LAST_UPDATE) {
    enum class Sort {
        LAST_UPDATE
    }

    abstract fun criteria(): Criteria

    data class All(
        val showDeleted: Boolean = false,
        val lastUpdatedFrom: Instant? = null,
        val lastUpdatedTo: Instant? = null,
    ) : ItemFilter() {
        override fun criteria(): Criteria {
            val criterions = if (showDeleted) {
                emptyList()
            } else {
                listOf(
                    Item::owner exists true,
                    Item::owner ne null
                )
            }

            return if(criterions.isEmpty()) {
                Criteria()
            } else {
                Criteria().andOperator(
                    criterions
                        .withFrom(lastUpdatedFrom)
                        .withTo(lastUpdatedTo)
                )
            }

        }

        private fun List<Criteria>.withFrom(from: Instant?): List<Criteria> {
            return if (from == null) {
                this
            } else {
                this + (Item::updatedAt gte from)
            }
        }

        private fun List<Criteria>.withTo(to: Instant?): List<Criteria> {
            return if (to == null) {
                this
            } else {
                this + (Item::updatedAt lt to)
            }
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
