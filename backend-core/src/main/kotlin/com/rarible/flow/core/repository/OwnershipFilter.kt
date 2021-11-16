package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.repository.filters.BuildsCriteria
import com.rarible.flow.core.repository.filters.ScrollingSort
import org.springframework.data.domain.Sort as SpringSort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo

sealed class OwnershipFilter: BuildsCriteria {

    enum class Sort: ScrollingSort<Ownership> {
        LATEST_FIRST;

        override fun springSort(): SpringSort {
            return SpringSort.by(
                SpringSort.Order.desc(Ownership::date.name),
                SpringSort.Order.desc(Ownership::id.name)
            )
        }

        override fun scroll(criteria: Criteria, continuation: String?): Criteria {
            return Cont.scrollDesc(criteria, continuation, Ownership::date, Ownership::id)
        }

        override fun nextPage(entity: Ownership): String {
            return Cont.toString(entity.date, entity.id)
        }
    }

    object All: OwnershipFilter() {
        override fun criteria(): Criteria = Criteria()
    }

    data class ByItem(val itemId: ItemId): OwnershipFilter() {
        override fun criteria(): Criteria {
            return Criteria().andOperator(
                Ownership::contract isEqualTo itemId.contract,
                Ownership::tokenId isEqualTo itemId.tokenId
            )
        }
    }
}
