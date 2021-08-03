package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId

sealed class OwnershipFilter {

    object All: OwnershipFilter()

    data class ByItem(val itemId: ItemId): OwnershipFilter()
}
