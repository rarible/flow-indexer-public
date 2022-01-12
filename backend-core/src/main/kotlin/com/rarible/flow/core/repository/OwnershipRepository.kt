package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.filters.DbFilter
import com.rarible.flow.core.repository.filters.ScrollingSort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface OwnershipRepository : ReactiveMongoRepository<Ownership, OwnershipId>, OwnershipRepositoryCustom {

    fun deleteAllByContractAndTokenId(contract: String, tokenId: TokenId /* = kotlin.Long */): Flux<Ownership>

    fun deleteAllByContractAndTokenIdAndOwnerNot(contract: String, tokenId: TokenId /* = kotlin.Long */, owner: FlowAddress): Flux<Ownership>

    fun findAllByContractAndTokenId(contract: String, tokenId: TokenId /* = kotlin.Long */): Flux<Ownership>
}

interface ScrollingRepository<T> {
    fun findByQuery(query: Query): Flux<T>

    fun defaultSort(): ScrollingSort<T>

    fun search(filter: DbFilter<T>, cont: String?, limit: Int?, sort: ScrollingSort<T> = defaultSort()): Flux<T> {
        return findByQuery(
            sort.scroll(filter, cont, limit)
        )
    }
}

interface OwnershipRepositoryCustom: ScrollingRepository<Ownership>

@Suppress("unused")
class OwnershipRepositoryImpl(private val mongoTemplate: ReactiveMongoTemplate): OwnershipRepositoryCustom {
    override fun defaultSort(): ScrollingSort<Ownership> {
        return OwnershipFilter.Sort.LATEST_FIRST
    }

    override fun findByQuery(query: Query): Flux<Ownership> {
        return mongoTemplate.find(query)
    }
}
