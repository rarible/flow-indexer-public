package com.rarible.flow.api.service

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.repository.OwnershipFilter
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coFindById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service

@Service
class OwnershipsService(
    private val ownershipRepository: OwnershipRepository
) {

    suspend fun byId(id: OwnershipId): Ownership? {
        return ownershipRepository.coFindById(id)
    }

    suspend fun all(continuation: String?, size: Int?, sort: OwnershipFilter.Sort): Flow<Ownership> {
        return ownershipRepository.search(OwnershipFilter.All, continuation, size, sort).asFlow()
    }

    suspend fun byItem(
        itemId: ItemId,
        continuation: String?,
        size: Int?,
        sort: OwnershipFilter.Sort
    ): Flow<Ownership> {
        return ownershipRepository.search(
            OwnershipFilter.ByItem(itemId),
            continuation,
            size,
            sort
        ).asFlow()
    }
}
