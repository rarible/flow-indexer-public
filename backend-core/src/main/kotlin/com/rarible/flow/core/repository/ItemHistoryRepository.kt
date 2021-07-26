package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.TokenId
import org.onflow.sdk.FlowAddress
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

/**
 * Item history repo
 */
@Repository
interface ItemHistoryRepository: ReactiveMongoRepository<ItemHistory, String> {

    @Query("{\$and: [{'activity.type': {\$in: ?0}}, {'activity.contract': {\$eq:  ?1}}, {'activity.tokenId': {\$eq: ?2}}]}", sort = "{'activity.date': -1}")
    fun getNftOrderActivitiesByItem(types: List<FlowActivityType>, contract: FlowAddress, tokenId: TokenId): Flux<ItemHistory>
}
