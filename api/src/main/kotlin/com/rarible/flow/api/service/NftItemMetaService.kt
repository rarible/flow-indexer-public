package com.rarible.flow.api.service

import com.rarible.flow.api.metaprovider.ItemMetaProvider
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NftItemMetaService(
    private val providers: List<ItemMetaProvider>,
    private val itemRepository: ItemRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getMetaByItemId(itemId: ItemId): ItemMeta? {
        val item = itemRepository.coFindById(itemId)

        if (item == null) {
            logger.warn("Unable to fetch meta for items that doesn't exists: $itemId")
            return null
        }

        val provider = providers.firstOrNull { it.isSupported(itemId) }
            ?: throw IllegalArgumentException("No meta provider found for item $itemId")

        return provider.getMeta(item)
    }
}
