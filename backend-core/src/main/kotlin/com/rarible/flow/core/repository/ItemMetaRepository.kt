package com.rarible.flow.core.repository;

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface ItemMetaRepository: ReactiveMongoRepository<ItemMeta, ItemId> {

    fun findAllByItemIdIn(ids: List<ItemId>): Flux<ItemMeta>
}
