package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository


interface OwnershipRepo: CoroutineCrudRepository<Ownership, OwnershipId> {
    suspend fun deleteAllByContractAndTokenId(contract: Address, tokenId: Int): Long
    suspend fun findAllByContractAndTokenId(contract: Address, tokenId: Int): Flow<Ownership>
}