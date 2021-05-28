package com.rarible.flow.scanner.repo

import com.rarible.flow.scanner.model.Item
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono


interface ItemRepositoryCustom {
    fun findLatest(): Mono<Item>
}

class ItemRepositoryCustomImpl(
    private val mongoTemplate: ReactiveMongoTemplate
): ItemRepositoryCustom {
    override fun findLatest(): Mono<Item> {
        val query = Query().limit(1).with(
            Sort.by(
                Sort.Direction.DESC,
                "updateTime"
            )
        )

        return mongoTemplate.findOne(query)
    }

}