package com.rarible.flow.api.royalty.provider

import com.rarible.flow.api.meta.provider.GamisodesMetaProvider
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
class GamisodesRoyaltyProvider(
    private val gamisodesMetaProvider: GamisodesMetaProvider
) : ItemRoyaltyProvider {

    override fun isSupported(itemId: ItemId): Boolean {
        return gamisodesMetaProvider.isSupported(itemId)
    }

    override suspend fun getRoyalties(item: Item): List<Royalty> {
        val meta = gamisodesMetaProvider.getGamisodesMeta(item) ?: return emptyList()
        return meta.royalties
    }
}
