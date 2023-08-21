package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.fetcher.RawOnChainMetaFetcher
import com.rarible.flow.api.service.meta.HWMetaEventTypeProvider
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

sealed class HotWheelsMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    parser: MattelMetaParser,
    metaEventTypeProvider: HWMetaEventTypeProvider
) : AbstractMetaProvider(
    fetcher,
    parser,
    metaEventTypeProvider
)

@Component
class HotWheelsCardMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: HWMetaEventTypeProvider
) : HotWheelsMetaProvider(fetcher, HotWheelsCardMetaParser, metaEventTypeProvider) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".HWGarageCard") || itemId.contract.endsWith(".HWGarageCardV2")

}

@Component
class HotWheelsPackMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: HWMetaEventTypeProvider,
) : HotWheelsMetaProvider(fetcher, HotWheelsPackMetaParser, metaEventTypeProvider) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".HWGaragePack") || itemId.contract.endsWith(".HWGaragePackV2")

}

@Component
class HotWheelsTokenMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: HWMetaEventTypeProvider,
) : HotWheelsMetaProvider(fetcher, HotWheelsPackMetaParser, metaEventTypeProvider) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".HWGarageTokenV2")
}
