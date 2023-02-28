package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.fetcher.HWMetaFetcher
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
class HotWheelsPackMetaProvider(fetcher: HWMetaFetcher) : HotWheelsMetaProvider(fetcher) {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".HWGaragePack")

    // "seriesName" - for v1, "carName" - for v2
    override val fieldName = fields("seriesName", "carName")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("thumbnailCID")
    override val fieldRights = fields()

    override val attributesWhiteList = setOf(
        // for v1
        "totalItemCount",
        // for v2
        "tokenReleaseDate",
        "tokenExpireDate"
    )
}