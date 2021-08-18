package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant


@Repository
interface OwnershipRepository : ReactiveMongoRepository<Ownership, OwnershipId>,
    ReactiveQuerydslPredicateExecutor<Ownership> {

    fun deleteAllByContractAndTokenId(contract: String, tokenId: TokenId /* = kotlin.Long */): Flux<Ownership>

    fun findAllByContractAndTokenIdOrderByDateDescContractDescTokenIdDesc(address: String, id: TokenId): Flux<Ownership>
}
