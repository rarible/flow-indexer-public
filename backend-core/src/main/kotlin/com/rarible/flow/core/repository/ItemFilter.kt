package com.rarible.flow.core.repository

import org.onflow.sdk.FlowAddress

sealed class ItemFilter(open val sort: Sort = Sort.LAST_UPDATE) {
    enum class Sort {
        LAST_UPDATE
    }

    data class All(val showDeleted: Boolean = false): ItemFilter()

    data class ByOwner(val owner: FlowAddress) : ItemFilter()

    data class ByCreator(val creator: FlowAddress): ItemFilter()

    data class ByCollection(val collectionId: String): ItemFilter()

}
