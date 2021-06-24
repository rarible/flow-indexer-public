package com.rarible.flow.scanner.repo

import com.rarible.flow.scanner.model.FlowBlock
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Repository
interface FlowBlockRepository: ReactiveMongoRepository<FlowBlock, String> {

    fun findTopByOrderByHeightDesc(): Mono<FlowBlock>

    @Aggregation(pipeline = [ "{\$group: { _id: '', total: {\$max: \$height }}}"])
    fun maxHeight(): Mono<Long>

    @Query(value = "{\$and : [{'height': {\$gte: ?0}}, {'height': {\$lte: ?1}}]}", fields = "{'height': 1}")
    fun heightsBetween(from: Long, to: Long): Flux<FlowBlock>
}
