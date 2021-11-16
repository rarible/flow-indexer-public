package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface OwnershipRepository : ReactiveMongoRepository<Ownership, OwnershipId>, OwnershipRepositoryCustom {

    fun deleteAllByContractAndTokenId(contract: String, tokenId: TokenId /* = kotlin.Long */): Flux<Ownership>

    fun deleteAllByContractAndTokenIdAndOwnerNot(contract: String, tokenId: TokenId /* = kotlin.Long */, owner: FlowAddress): Flux<Ownership>
}

interface OwnershipRepositoryCustom {
    fun search(
        filter: OwnershipFilter, cont: String?, limit: Int?, sort: OwnershipFilter.Sort = OwnershipFilter.Sort.LATEST_FIRST
    ): Flux<Ownership>
}

@Suppress("unused")
class OwnershipRepositoryImpl(private val mongoTemplate: ReactiveMongoTemplate): OwnershipRepositoryCustom {
    override fun search(filter: OwnershipFilter, cont: String?, limit: Int?, sort: OwnershipFilter.Sort): Flux<Ownership> {
        val query = sort.scroll(filter, cont, limit)
        return mongoTemplate.find(query)
    }
}
