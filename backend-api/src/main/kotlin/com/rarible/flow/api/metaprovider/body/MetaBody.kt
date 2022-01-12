package com.rarible.flow.api.metaprovider.body

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta


interface MetaBody {
    fun toItemMeta(itemId: ItemId): ItemMeta
}