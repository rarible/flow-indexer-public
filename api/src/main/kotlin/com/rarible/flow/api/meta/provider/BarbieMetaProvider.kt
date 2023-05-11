package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.fetcher.RawOnChainMetaFetcher
import com.rarible.flow.api.service.meta.BarbieMetaEventTypeProvider
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

sealed class BarbieMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    parser: MattelMetaParser,
    metaEventTypeProvider: BarbieMetaEventTypeProvider,
) : AbstractMetaProvider(
    fetcher,
    parser,
    metaEventTypeProvider
)

@Component
class BarbieCardMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: BarbieMetaEventTypeProvider,
) : BarbieMetaProvider(fetcher, BarbieCardMetaParser, metaEventTypeProvider) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".BBxBarbieCard")

}

@Component
class BarbiePackMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: BarbieMetaEventTypeProvider,
) : BarbieMetaProvider(fetcher, BarbiePackMetaParser, metaEventTypeProvider) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".BBxBarbiePack")

}

@Component
class BarbieTokenMetaProvider(
    fetcher: RawOnChainMetaFetcher,
    metaEventTypeProvider: BarbieMetaEventTypeProvider,
) : BarbieMetaProvider(fetcher, BarbieTokenMetaParser, metaEventTypeProvider) {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.endsWith(".BBxBarbieToken")

}