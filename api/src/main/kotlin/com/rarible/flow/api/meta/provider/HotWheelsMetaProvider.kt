package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.fetcher.RawOnChainMetaFetcher
import com.rarible.flow.api.service.meta.HWMetaEventTypeProvider
import com.rarible.flow.core.config.FeatureFlagsProperties
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

sealed class HotWheelsMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    parser: MattelMetaParser,
    metaEventTypeProvider: HWMetaEventTypeProvider,
    ff: FeatureFlagsProperties
) : AbstractMetaProvider(
    fetcher,
    parser,
    metaEventTypeProvider
) {

    override val defaultContentType = if (ff.enableHotWheelsDefaultVideoContentType) {
        ItemMetaContent.Type.VIDEO
    } else {
        ItemMetaContent.Type.IMAGE
    }
}

@Component
class HotWheelsCardMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: HWMetaEventTypeProvider,
    ff: FeatureFlagsProperties
) : HotWheelsMetaProvider(fetcher, HotWheelsCardMetaParser, metaEventTypeProvider, ff) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".HWGarageCard") || itemId.contract.endsWith(".HWGarageCardV2")

}

@Component
class HotWheelsPackMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    ff: FeatureFlagsProperties,
    metaEventTypeProvider: HWMetaEventTypeProvider,
) : HotWheelsMetaProvider(fetcher, HotWheelsPackMetaParser, metaEventTypeProvider, ff) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".HWGaragePack") || itemId.contract.endsWith(".HWGaragePackV2")

}