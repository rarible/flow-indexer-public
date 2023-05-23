package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.fetcher.RawOnChainMetaFetcher
import com.rarible.flow.api.service.meta.BarbieMetaEventTypeProvider
import com.rarible.flow.core.config.FeatureFlagsProperties
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

sealed class BarbieMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    parser: MattelMetaParser,
    metaEventTypeProvider: BarbieMetaEventTypeProvider,
    ff: FeatureFlagsProperties
) : AbstractMetaProvider(
    fetcher,
    parser,
    metaEventTypeProvider
) {

    override val defaultContentType = if (ff.enableBarbieDefaultVideoContentType) {
        ItemMetaContent.Type.VIDEO
    } else {
        ItemMetaContent.Type.IMAGE
    }
}

@Component
class BarbieCardMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: BarbieMetaEventTypeProvider,
    ff: FeatureFlagsProperties
) : BarbieMetaProvider(fetcher, BarbieCardMetaParser, metaEventTypeProvider, ff) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".BBxBarbieCard")

}

@Component
class BarbiePackMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: BarbieMetaEventTypeProvider,
    ff: FeatureFlagsProperties
) : BarbieMetaProvider(fetcher, BarbiePackMetaParser, metaEventTypeProvider, ff) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".BBxBarbiePack")

}

@Component
class BarbieTokenMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: BarbieMetaEventTypeProvider,
    ff: FeatureFlagsProperties
) : BarbieMetaProvider(fetcher, BarbieTokenMetaParser, metaEventTypeProvider, ff) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".BBxBarbieToken")

}