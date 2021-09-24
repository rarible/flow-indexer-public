package com.rarible.flow.listener.service

import com.mongodb.client.result.UpdateResult
import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ItemService(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository
) {
    suspend fun markDeleted(itemId: ItemId): UpdateResult {
        return itemRepository.updateById(itemId, Update().unset(Item::owner.name))
    }

    suspend fun unlist(itemId: ItemId): UpdateResult {
        return itemRepository.updateById(itemId, Update().set(Item::listed.name, false))
    }

    suspend fun byId(itemId: ItemId): Item? = itemRepository.findById(itemId).awaitSingleOrNull()

    suspend fun findAliveById(itemId: ItemId): Item? =
        itemRepository.findByIdAndOwnerIsNotNullOrderByMintedAtDescTokenIdDesc(itemId).awaitSingleOrNull()

    suspend fun transferNft(itemId: ItemId, to: FlowAddress): Pair<Item, Ownership>? {
        return itemRepository.coFindById(itemId)?.let { item ->
            itemRepository.coSave(item.transfer(to))
        }?.let { item ->
            ownershipRepository
                .deleteAllByContractAndTokenId(item.contract, item.tokenId)
                .collectList().awaitFirstOrDefault(emptyList())

            val ownership = ownershipRepository.coSave(
                Ownership(item.contract, item.tokenId, to, Instant.now(), listOf(Payout(account = item.creator, value = 100.toBigDecimal())))
            )
            item to ownership
        } ?: run {
            log.warn("Not existing NFTs [{}] transfer to [{}]", itemId, to.formatted)
            null
        }
    }

    companion object {
        val log by Log()
    }
}
