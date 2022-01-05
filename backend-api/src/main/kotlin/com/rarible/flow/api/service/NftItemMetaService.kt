package com.rarible.flow.api.service

import com.rarible.flow.api.metaprovider.ItemMetaProvider
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class NftItemMetaService(
    private val providers: List<ItemMetaProvider>,
    private val itemMetaRepository: ItemMetaRepository,
    private val itemRepository: ItemRepository
) {

    suspend fun getMetaByItemId(itemId: ItemId): ItemMeta {
        val exists = itemMetaRepository.coFindById(itemId)
        return if (exists == null) {
            val meta = getMeta(
                providers.firstOrNull { it.isSupported(itemId) },
                itemRepository.coFindById(itemId)
            ) ?: ItemMeta.empty(itemId)
            return itemMetaRepository.coSave(meta)
        } else {
            exists
        }
    }

    suspend fun getMeta(provider: ItemMetaProvider?, item: Item?): ItemMeta? {
        return if (provider == null || item == null) null
        else provider.getMeta(item)
    }

    suspend fun resetMeta(itemId: ItemId) {
        itemMetaRepository.deleteById(itemId).awaitFirstOrNull()
    }
}
