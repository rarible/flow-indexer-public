package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import org.onflow.sdk.FlowAddress
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface ItemRepository: ReactiveMongoRepository<Item, ItemId> {
    fun findAllByOwner(owner: FlowAddress): Flux<Item>

    fun findAllByCreator(creator: FlowAddress): Flux<Item>

    fun findAllByListedIsTrue(): Flux<Item>
}