package com.rarible.flow.api.service

import com.rarible.flow.api.metaprovider.ItemMetaProvider
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Service

@Service
class NftItemMetaService(
    private val providers: List<ItemMetaProvider>,
    private val itemMetaRepository: ItemMetaRepository,
    private val itemRepository: ItemRepository
) {
    private val logger by Log()

    suspend fun getMetaByItemId(itemId: ItemId): ItemMeta {
        val exists = itemMetaRepository.coFindById(itemId)
        return if (exists == null) {
            val meta = getMeta(
                providers.firstOrNull { it.isSupported(itemId) },
                itemRepository.coFindById(itemId)
            )

            return if (meta == null) {
                logger.warn("No meta or meta provider is found for item [{}]", itemId)
                ItemMeta.empty(itemId)
            } else {
                itemMetaRepository.coSave(meta)
            }
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
