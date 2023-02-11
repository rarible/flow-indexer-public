package com.rarible.flow.core.repository

import com.rarible.blockchain.scanner.block.Block
import com.rarible.blockchain.scanner.block.BlockStatus
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono


interface ExtendedFlowBlockRepository: ReactiveCrudRepository<Block, Long> {

    fun findFirstByOrderByIdAsc(): Mono<Block>

    fun findFirstByOrderByIdDesc(): Mono<Block>

    fun countByStatus(status: BlockStatus): Mono<Long>
}