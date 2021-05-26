package com.rarible.flow.scanner.repo

import com.rarible.flow.scanner.model.FlowBlock
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigInteger

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Repository
interface FlowBlockRepository: ReactiveMongoRepository<FlowBlock, BigInteger>, ReactiveQueryByExampleExecutor<FlowBlock> {

    fun findTopByOrderByHeightDesc(): Mono<FlowBlock>

    @Tailable
    @Query("{}")
    fun findAllBlocks(): Flux<FlowBlock>
}
