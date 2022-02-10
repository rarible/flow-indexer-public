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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
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
        val item = itemRepository.coFindById(itemId) ?: return ItemMeta.empty(itemId).let {
            logger.warn("Unable to fetch meta for ItemId[$itemId]! Item not found!")
            it
        }
        val exists = itemMetaRepository.coFindById(itemId)
        return if (exists == null) {
            val meta = flow {
                emit(
                    providers.first { it.isSupported(itemId) }.getMeta(item)
                )
            }
            .retry(retries = 3L) { failure ->
                logger.warn("Retrying to get meta fot [{}] in 2s...", itemId, failure)
                delay(2000)
                true
            }
            .catch { err ->
                logger.error("Failed to get meta data for item: {}", itemId, err)
                emit(ItemMeta.empty(itemId))
            }
            .first()

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
