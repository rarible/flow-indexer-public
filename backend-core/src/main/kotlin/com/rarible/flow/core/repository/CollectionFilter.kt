package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.filters.BuildsCriteria
import com.rarible.flow.core.repository.filters.ScrollingSort
import org.springframework.data.domain.Sort as SpringSort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo


sealed class CollectionFilter: BuildsCriteria {

    enum class Sort: ScrollingSort<ItemCollection> {
        LATEST_UPDATE {
            override fun springSort(): org.springframework.data.domain.Sort {
                return SpringSort.by(
                    SpringSort.Order.desc(ItemCollection::createdDate.name),
                    SpringSort.Order.desc(ItemCollection::id.name),
                )
            }

            override fun scroll(criteria: Criteria, continuation: String?): Criteria {
                return Cont.scrollDesc(criteria, continuation, ItemCollection::createdDate, ItemCollection::id)
            }

            override fun nextPage(entity: ItemCollection): String {
                return Cont.toString(entity.createdDate, entity.id)
            }
        }
    }

    object All: CollectionFilter() {
        override fun criteria(): Criteria = Criteria()
    }

    data class ByOwner(val owner: FlowAddress): CollectionFilter() {
        override fun criteria(): Criteria {
            return ItemCollection::owner isEqualTo this.owner
        }
    }


}
