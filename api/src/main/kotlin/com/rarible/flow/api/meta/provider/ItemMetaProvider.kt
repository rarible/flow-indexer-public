package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId

interface ItemMetaProvider {
    fun isSupported(itemId: ItemId): Boolean
    suspend fun getMeta(item: Item): ItemMeta?
    fun emptyMeta(itemId: ItemId): ItemMeta = ItemMeta(
        itemId = itemId,
        name = "Untitled",
        description = "",
        attributes = emptyList(),
        contentUrls = emptyList()
    )
}
