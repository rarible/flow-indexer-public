package com.rarible.flow.core.repository

import org.onflow.sdk.FlowAddress

sealed class ItemFilter(open val sort: Sort = Sort.LAST_UPDATE) {
    enum class Sort {
        LAST_UPDATE
    }

    object All: ItemFilter()

    data class ByOwner(val owner: FlowAddress) : ItemFilter()

    data class ByCreator(val creator: FlowAddress): ItemFilter()

}