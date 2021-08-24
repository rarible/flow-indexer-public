package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemCollection
import org.onflow.sdk.FlowAddress
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo


sealed class CollectionFilter {

    abstract fun criteria(): Criteria

    object All: CollectionFilter() {
        override fun criteria(): Criteria = Criteria()
    }

    data class ByOwner(val owner: FlowAddress): CollectionFilter() {
        override fun criteria(): Criteria {
            return ItemCollection::owner isEqualTo this.owner
        }
    }


}