package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import org.onflow.sdk.FlowAddress
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant


interface OwnershipRepository : ReactiveMongoRepository<Ownership, OwnershipId> {

    fun deleteAllByContractAndTokenId(address: FlowAddress, tokenId: TokenId): Mono<Void>

    fun findAllByContractAndTokenIdOrderByDateDesc(address: FlowAddress, id: TokenId): Flux<Ownership>

    fun findAllByDateAfterAndIdNotOrderByDateDesc(after: Instant, id: OwnershipId): Flux<Ownership>

    fun findAllByContractAndTokenIdAndDateAfterAndIdNotOrderByDateDesc(contract: FlowAddress, tokenId: TokenId /* = kotlin.Long */, afterDate: Instant, id: OwnershipId): Flux<Ownership>

}
