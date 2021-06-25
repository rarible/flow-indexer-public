package com.rarible.flow.scanner.repo

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono


//@Repository
//interface  ItemRepository: ReactiveMongoRepository<Item, String> {
//    fun findTopByOrderByUpdateTimeDesc(): Mono<Item>
//}