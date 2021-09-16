package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.ne


sealed class ItemFilter(open val sort: Sort = Sort.LAST_UPDATE) {
    enum class Sort {
        LAST_UPDATE
    }

    abstract fun criteria(): Criteria

    data class All(val showDeleted: Boolean = false): ItemFilter() {
        override fun criteria(): Criteria {
            return if (showDeleted) Criteria() else {
                Criteria().andOperator(
                    Item::owner exists true,
                    Item::owner ne null
                )
            }
        }
    }

    data class ByOwner(val owner: FlowAddress) : ItemFilter() {
        override fun criteria(): Criteria {
            return Item::owner isEqualTo owner
        }
    }

    data class ByCreator(val creator: FlowAddress): ItemFilter() {
        override fun criteria(): Criteria {
            return (Item::creator isEqualTo creator).andOperator(
                All(false).criteria()
            )
        }
    }

    data class ByCollection(val collectionId: String): ItemFilter() {
        override fun criteria(): Criteria {
            return (Item::collection isEqualTo collectionId).andOperator(
                All(false).criteria()
            )
        }
    }

}
