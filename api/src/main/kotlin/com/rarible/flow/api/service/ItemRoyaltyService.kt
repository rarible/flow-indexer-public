package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.royalty.provider.ItemRoyaltyProvider
import com.rarible.flow.api.royalty.provider.Royalty
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.repository.withEntity
import org.springframework.stereotype.Service

@Service
class ItemRoyaltyService(
    private val providers: List<ItemRoyaltyProvider>,
    private val itemRepository: ItemRepository,
) {

    suspend fun getRoyaltiesByItemId(itemId: ItemId): List<Royalty>? = itemRepository.withEntity(itemId) { item ->
        item.royalties
            .map { Royalty(it.address.formatted, it.fee.toBigDecimal()) }
            .ifEmpty { getRoyalties(itemId)?.also { item.saveRoyalties(it) } }
    }

    suspend fun resetRoyalties(itemId: ItemId): Item? = itemRepository.withEntity(itemId) { item ->
        item.saveRoyalties(emptyList())
    }

    private suspend fun getRoyalties(itemId: ItemId): List<Royalty>? =
        providers.find { it.isSupported(itemId) }?.let { provider ->
            itemRepository.withEntity(itemId, provider::getRoyalties)
        }

    private suspend fun Item.saveRoyalties(royalties: List<Royalty>) =
        itemRepository.coSave(copy(royalties = royalties.map { Part(FlowAddress(it.address), it.fee.toDouble()) }))

}
