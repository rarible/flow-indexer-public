package com.rarible.flow.api.meta.provider.legacy

import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.core.domain.ItemId

interface MetaBody {
    fun toItemMeta(itemId: ItemId): ItemMeta
}