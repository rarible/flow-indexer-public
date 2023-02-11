package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.filters.DbFilter
import com.rarible.flow.core.repository.filters.ScrollingSort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.data.domain.Sort as SpringSort


sealed class CollectionFilter: DbFilter<ItemCollection> {

    enum class Sort: ScrollingSort<ItemCollection> {
        BY_ID {
            override fun springSort(): org.springframework.data.domain.Sort {
                return SpringSort.by(
                    SpringSort.Order.desc(ItemCollection::chainId.name),
                    SpringSort.Order.desc(ItemCollection::id.name),
                )
            }

            override fun scroll(criteria: Criteria, continuation: String?): Criteria {
                return if (continuation == null) {
                    criteria
                } else {
                    val parts = continuation.split('.')
                    if (parts.size == 4 && parts.last().toLong() > 0) {
                        criteria.orOperator(
                            where(ItemCollection::chainId).lt(parts.last().toLong()),
                            where(ItemCollection::isSoft).isEqualTo(false)
                        )
                    } else {
                        criteria.and(ItemCollection::id).lt(continuation).and(ItemCollection::isSoft).isEqualTo(false)
                    }

                }
            }

            override fun nextPage(entity: ItemCollection): String {
                return entity.id
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
