package com.rarible.flow.api.metaprovider

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import org.springframework.stereotype.Component

@Component
class StarlyMetaProvider(

): ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("StarlyCard")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        TODO("Not yet implemented")
    }
}