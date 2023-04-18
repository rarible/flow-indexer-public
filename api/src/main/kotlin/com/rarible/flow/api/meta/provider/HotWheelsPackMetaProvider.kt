package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.fetcher.HWMetaFetcher
import com.rarible.flow.api.meta.getFirst
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
class HotWheelsPackMetaProvider(fetcher: HWMetaFetcher) : HotWheelsMetaProvider(fetcher) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".HWGaragePack") || itemId.contract.endsWith(".HWGaragePackV2")

    override fun getName(map: Map<String, String>): String? {
        return map.getFirst(*fieldName)
    }

    // "seriesName" - for v1, "carName" - for v2, packName - for V2
    override val fieldName = fields("seriesName", "carName", "packName")
    override val fieldDescription = fields("packDescription")
    override val fieldImageOriginal = fields("thumbnailCID")
    override val fieldRights = fields()

    override val attributesWhiteList = setOf(
        // for v1
        "totalItemCount",
        // for v2
        "tokenReleaseDate",
        "tokenExpireDate",
        "collectionName",
        "collectionDescription",
    )
}