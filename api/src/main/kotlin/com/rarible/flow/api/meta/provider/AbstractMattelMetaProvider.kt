package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.MetaException
import com.rarible.flow.api.meta.fetcher.RawOnChainMetaFetcher
import com.rarible.flow.api.service.meta.MetaEventTypeProvider
import com.rarible.flow.core.domain.Item

abstract class AbstractMattelMetaProvider(
    private val fetcher: RawOnChainMetaFetcher,
    private val parser: MattelMetaParser,
    private val metaEventTypeProvider: MetaEventTypeProvider,
) : ItemMetaProvider {

    override suspend fun getMeta(item: Item): ItemMeta? {
        val json = fetcher.getContent(item.id, metaEventTypeProvider.getMetaEventType(item.id))
            ?: throw MetaException("Item ${item.id} doesn't have meta json", MetaException.Status.NOT_FOUND)

        return parser.parse(json, item.id)
    }
}
