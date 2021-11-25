package com.rarible.flow.core.repository

import com.rarible.blockchain.scanner.flow.model.FlowBlock
import com.rarible.blockchain.scanner.framework.model.Block
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono


interface ExtendedFlowBlockRepository: ReactiveCrudRepository<FlowBlock, Long> {

    fun findFirstByOrderByIdAsc(): Mono<FlowBlock>

    fun findFirstByOrderByIdDesc(): Mono<FlowBlock>

    fun countByStatus(status: Block.Status): Mono<Long>
}