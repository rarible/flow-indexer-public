package com.rarible.flow.api.service

import com.rarible.flow.api.metaprovider.ItemMetaProvider
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onErrorReturn
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Service

@Service
class NftItemMetaService(
    private val providers: List<ItemMetaProvider>,
    private val itemMetaRepository: ItemMetaRepository
) {

    private val logger by Log()

    suspend fun getMetaByItemId(itemId: ItemId): ItemMeta {
        val exists = itemMetaRepository.coFindById(itemId)
        return if (exists == null) {
            val meta = flow<ItemMeta> {
                providers.first { it.isSupported(itemId) }.getMeta(itemId)
            }
            .retry(retries = 3L) { failure ->
                logger.warn("Retrying to get meta fot [{}] in 2s...", itemId, failure)
                delay(2000)
                true
            }
            .catch { emit(ItemMeta.empty(itemId)) }
            .first()

            return itemMetaRepository.coSave(meta)
        } else {
            exists
        }
    }

    suspend fun resetMeta(itemId: ItemId) {
        itemMetaRepository.deleteById(itemId).awaitFirstOrNull()
    }
}
