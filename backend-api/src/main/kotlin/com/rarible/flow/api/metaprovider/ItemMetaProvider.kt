package com.rarible.flow.api.metaprovider

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta

interface ItemMetaProvider {

    fun isSupported(itemId: ItemId): Boolean

    suspend fun getMeta(itemId: ItemId): ItemMeta

    fun emptyMeta(itemId: ItemId): ItemMeta = ItemMeta(
        itemId = itemId,
        name = "Untitled",
        description = "",
        attributes = emptyList(),
        contentUrls = emptyList()
    )
}
