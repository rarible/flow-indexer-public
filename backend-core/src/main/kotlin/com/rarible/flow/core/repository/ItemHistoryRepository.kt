package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.TokenId
import org.onflow.sdk.FlowAddress
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

/**
 * Item history repo
 */
@Repository
interface ItemHistoryRepository: ReactiveMongoRepository<ItemHistory, String> {

    @Query("{\$and: [{'activity.type': {\$in: ?0}}, {'activity.contract': {\$eq:  ?1}}, {'activity.tokenId': {\$eq: ?2}}]}", sort = "{'activity.date': -1}")
    fun getNftOrderActivitiesByItem(types: List<FlowActivityType>, contract: FlowAddress, tokenId: TokenId): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$in: ?0}}, {'activity.contract': {\$eq:  ?1}}, {'activity.tokenId': {\$eq: ?2}}, {'activity.date': {\$gt: ?3}}]}", sort = "{'activity.date': -1}")
    fun getNftOrderActivitiesByItemAfterDate(types: List<FlowActivityType>, contract: FlowAddress, tokenId: TokenId, afterDate: Instant): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$in: ?0}}, {'activity.owner': {\$in:  ?1}}]}", sort = "{'activity.date': -1}")
    fun getNftOrderActivitiesByUser(types: List<FlowActivityType>, user: List<FlowAddress>): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$in: ?0}}, {'activity.owner': {\$in:  ?1}}, {'activity.date: {\$gt: ?2}'}]}", sort = "{'activity.date': -1}")
    fun getNftOrderActivitiesByUserAfterDate(types: List<FlowActivityType>, users: List<FlowAddress>, afterDate: Instant): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$eq: 'TRANSFER'}}, {'activity.owner': {\$in: ?0}}]}", sort = "{'activity.date': -1}")
    fun getNfrOrderTransferToActivitiesByUser(user: List<FlowAddress>): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$eq: 'TRANSFER'}}, {'activity.owner': {\$in: ?0}}, {'activity.date: {\$gt: ?1}'}]}", sort = "{'activity.date': -1}")
    fun getNfrOrderTransferToActivitiesByUserAfterDate(users: List<FlowAddress>, afterDate: Instant): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$eq: 'TRANSFER'}}, {'activity.from': {\$in: ?0}}]}", sort = "{'activity.date': -1}")
    fun getNftOrderTransferFromActivitiesByUser(user: List<FlowAddress>): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$eq: 'TRANSFER'}}, {'activity.from': {\$in: ?0}}, {'activity.date: {\$gt: ?1}'}]}", sort = "{'activity.date': -1}")
    fun getNftOrderTransferFromActivitiesByUserAfterDate(users: List<FlowAddress>, afterDate: Instant): Flux<ItemHistory>

    @Query("{'activity.type': {\$in: ?0}}", sort = "{'activity.date': -1}")
    fun getAllActivities(types: List<FlowActivityType>): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$in: ?0}}, {'activity.date': {\$gt: ?1}}]}", sort = "{'activity.date': -1}")
    fun getAllActivitiesAfterDate(types: List<FlowActivityType>, afterDate: Instant): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$in: ?0}}, {'activity.collection': {\$eq: ?1}}]}", sort = "{'activity.date': -1}")
    fun getAllActivitiesByItemCollection(types: List<FlowActivityType>, collection: String): Flux<ItemHistory>

    @Query("{\$and: [{'activity.type': {\$in: ?0}}, {'activity.collection': {\$eq: ?1}}, {'activity.date: {\$gt: ?2}'}]}", sort = "{'activity.date': -1}")
    fun getAllActivitiesByItemCollectionAfterDate(types: List<FlowActivityType>, collection: String, afterDate: Instant): Flux<ItemHistory>


}
