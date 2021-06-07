package com.rarible.flow.scanner.repo

import com.rarible.flow.scanner.model.FlowBlock
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Repository
interface FlowBlockRepository: ReactiveMongoRepository<FlowBlock, String> {

    fun findTopByOrderByHeightDesc(): Mono<FlowBlock>

    fun findTopByOrderByHeightAsc(): Mono<FlowBlock>
}
