package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.fetcher.HWMetaFetcher
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
class HotWheelsCardMetaProvider(fetcher: HWMetaFetcher) : HotWheelsMetaProvider(fetcher) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".HWGarageCard") || itemId.contract.endsWith(".HWGarageCardV2")

    override fun getName(map: Map<String, String>): String? {
        return map.get("carName") + " #" +  map.get("cardId")
    }

    override val fieldName = fields("carName")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("imageUrl")
    override val fieldRights = fields("licensorLegal")

    override val attributesWhiteList = setOf(
        "seriesName",
        "releaseYear",
        "rarity",
        "redeemable",
        "type",
        "mint",
        "totalSupply",
        "cardId",
        "miniCollection"
    )

}