package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.CollectionFilter
import com.rarible.flow.core.repository.ItemCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class CollectionService(
    private val repo: ItemCollectionRepository
) {

    suspend fun byId(collectionId: String): ItemCollection? =
        repo.findById(collectionId).awaitSingleOrNull()

    fun searchAll(continuation: String?, size: Int?): Flow<ItemCollection> {
        return repo.search(
            CollectionFilter.All, continuation, size, CollectionFilter.Sort.BY_ID
        ).asFlow()
    }

    fun searchByOwner(owner: FlowAddress, continuation: String?, size: Int?): Flow<ItemCollection> {
        return repo.search(
            CollectionFilter.ByOwner(owner), continuation, size, CollectionFilter.Sort.BY_ID
        ).asFlow()
    }

    fun byIds(ids: List<String>): Flow<ItemCollection> {
        return repo.findAllById(ids).asFlow()
    }
}
