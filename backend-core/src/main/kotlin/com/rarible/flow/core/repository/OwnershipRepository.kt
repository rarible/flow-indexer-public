package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor
import reactor.core.publisher.Flux

interface OwnershipRepository : ReactiveMongoRepository<Ownership, OwnershipId>,
    ReactiveQuerydslPredicateExecutor<Ownership>, OwnershipRepositoryCustom {

    fun deleteAllByContractAndTokenId(contract: String, tokenId: TokenId /* = kotlin.Long */): Flux<Ownership>
}

interface OwnershipRepositoryCustom {

    fun all(continuation: String?, size: Int?): Flux<Ownership>

    fun byItem(contract: String, tokenId: TokenId, continuation: String?, size: Int?): Flux<Ownership>
}

@Suppress("unused")
class OwnershipRepositoryImpl(private val mongoTemplate: ReactiveMongoTemplate): OwnershipRepositoryCustom {
    override fun all(continuation: String?, size: Int?): Flux<Ownership> {
        val query = Query().limit(size ?: DEFAULT_LIMIT)
        val criteria = Criteria()
        addContinuation(criteria, OwnershipContinuation.of(continuation))
        query.addCriteria(criteria).with(Sort.by(Sort.Direction.DESC, "date"))
        return mongoTemplate.find(query, Ownership::class.java)
    }

    override fun byItem(contract: String, tokenId: TokenId, continuation: String?, size: Int?): Flux<Ownership> {
        val query = Query().limit(size ?: DEFAULT_LIMIT)
        val criteria = Criteria.where("contract").isEqualTo(contract)
            .and("tokenId").isEqualTo(tokenId)

        addContinuation(criteria, OwnershipContinuation.of(continuation))
        query.addCriteria(criteria).with(Sort.by(Sort.Direction.DESC, "date"))
        return mongoTemplate.find(query, Ownership::class.java)
    }

    private fun addContinuation(criteria: Criteria, continuation: OwnershipContinuation?) {
        if (continuation != null) {
            criteria.and("date").lte(continuation.beforeDate).and("id").ne(continuation.beforeId)
        }
    }
}
