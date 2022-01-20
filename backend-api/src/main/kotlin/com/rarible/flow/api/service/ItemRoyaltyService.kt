package com.rarible.flow.api.service

import com.rarible.flow.api.royaltyprovider.ItemRoyaltyProvider
import com.rarible.flow.api.royaltyprovider.Royalty
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSave
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class ItemRoyaltyService(
    private val providers: List<ItemRoyaltyProvider>,
    private val itemRepository: ItemRepository,
) {

    suspend fun getRoyaltyByItemId(itemId: ItemId): List<Royalty>? = withItem(itemId) { item ->
        item.royalties
            .map { Royalty(it.address.formatted, it.fee.toBigDecimal()) }
            .ifEmpty { getRoyalty(itemId) }
    }

    suspend fun resetRoyalty(itemId: ItemId): Item? = withItem(itemId) {
        itemRepository.coSave(it.copy(royalties = emptyList()))
    }

    private suspend fun getRoyalty(itemId: ItemId): List<Royalty>? =
        providers.find { it.isSupported(itemId) }?.let { provider ->
            withItem(itemId, provider::getRoyalty)
        }

    private suspend fun <T> withItem(itemId: ItemId, block: suspend (Item) -> T): T? =
        itemRepository.findById(itemId).awaitSingleOrNull()?.let { block(it) }
}
