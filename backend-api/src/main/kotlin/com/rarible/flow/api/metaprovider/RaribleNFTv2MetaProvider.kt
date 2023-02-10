package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.event.RaribleNFTv2Meta
import org.springframework.stereotype.Component

@Component
class RaribleNFTv2MetaProvider: ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".RaribleNFTv2")

    override suspend fun getMeta(item: Item): ItemMeta? {
        val meta = jacksonObjectMapper().readValue<RaribleNFTv2Meta>(item.meta!!)
        return ItemMeta(
            itemId = item.id,
            name = meta.name,
            description = meta.description.orEmpty(),
            attributes = meta.attributes.map {
                ItemMetaAttribute(
                    key = it.key,
                    value = it.value
                )
            },
            contentUrls = meta.contentUrls
        )
    }

}
