package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.CollectionFilter
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.core.repository.NftCollectionContinuation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class CollectionService(
    private val repo: ItemCollectionRepository
) {

    suspend fun byId(collectionId: String): ItemCollection? =
        repo.findById(collectionId).awaitSingleOrNull()

    fun searchAll(continuation: NftCollectionContinuation?, size: Int?): Flow<ItemCollection> {
        return repo.search(CollectionFilter.All, continuation, size)
    }

    fun searchByOwner(owner: FlowAddress, continuation: NftCollectionContinuation?, size: Int?): Flow<ItemCollection> {
        return repo.search(CollectionFilter.ByOwner(owner), continuation, size)
    }


}
