package com.rarible.flow.api.service

import com.rarible.flow.api.metaprovider.ItemMetaProvider
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.coFindById
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class NftItemMetaService(
    private val providers: List<ItemMetaProvider>,
    private val itemMetaRepository: ItemMetaRepository
) {

    suspend fun getMetaByItemId(itemId: ItemId): ItemMeta? {
        val exists = itemMetaRepository.coFindById(itemId)
        return if (exists == null) {
            val meta = providers.firstOrNull { it.isSupported(itemId) }?.getMeta(itemId) ?: return null
            return itemMetaRepository.save(meta).awaitSingle()
        } else {
            exists
        }
    }

    suspend fun resetMeta(itemId: ItemId) {
        itemMetaRepository.deleteById(itemId).subscribe()
    }
}
