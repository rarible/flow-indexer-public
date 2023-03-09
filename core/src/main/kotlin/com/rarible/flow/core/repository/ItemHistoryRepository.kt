package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.TokenId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Item history repo
 */
@Repository
interface ItemHistoryRepository : ReactiveMongoRepository<ItemHistory, String> {

    @Suppress("FunctionName")
    fun existsByLog_TransactionHashAndLog_EventIndex(txHash: String, eventIndex: Int): Mono<Boolean>

    @Query(
        """
            {"activity.type": ?0, "activity.hash": ?1}
        """
    )
    fun findOrderActivity(type: String, hash: String): Flux<ItemHistory>

    @DeleteQuery(
        """
            {"activity.contract": ?0, "activity.tokenId": ?1}
        """)
    fun deleteByItemId(contract: String, tokenId: Long): Flux<ItemHistory>

    @Query("""
            {"activity.contract": ?0, "activity.tokenId": ?1}
        """)
    fun findByItemId(contract: String, tokenId: Long): Flux<ItemHistory>

    @Query("""
            {"log.transactionHash": ?0, "activity.type": "SELL"}, ${"$"}or: [
                { "activity.left.maker": ?1, "activity.right.maker": ?2 },
                { "activity.left.maker": ?2, "activity.right.maker": ?1 }
            ] }
        """)
    fun findOrderInTx(txHash: String, from: String, to: String): Flux<ItemHistory>

    @Query("""
            {"log.transactionHash": ?0, "activity.type": "TRANSFER", "activity.from": ?1, "activity.to": ?2}
        """)
    fun findTransferInTx(txHash: String, from: String, to: String): Flux<ItemHistory>

    @Query("""
            {"activity.contract": ?0, "activity.tokenId": ?1, "activity.type": ?2}
        """)
    fun findItemActivity(contract: String, tokenId: TokenId, type: FlowActivityType): Flux<ItemHistory>
}

class TaskItemHistoryRepository(
    private val mongo: ReactiveMongoTemplate
) {
    suspend fun delete(entity: ItemHistory) {
        mongo.remove(entity).awaitFirstOrNull()
    }

    fun find(fromId: String?, limit: Int): Flow<ItemHistory> {
        val criteria = if (fromId != null) Criteria.where("_id").gt(fromId) else Criteria()
        val query = org.springframework.data.mongodb.core.query.Query(criteria)
            .with(Sort.by("_id"))
            .limit(limit)

        return mongo.find<ItemHistory>(query).asFlow()
    }
}

suspend fun ItemHistoryRepository.findItemMint(contract: String, tokenId: TokenId): List<ItemHistory> {
    return findItemActivity(contract, tokenId, FlowActivityType.MINT).collectList().awaitSingle()
}
