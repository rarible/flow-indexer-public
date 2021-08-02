package com.rarible.flow.api.service

import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.protocol.dto.FlowNftCollectionDto
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CollectionService(
    private val repo: ItemCollectionRepository
) {

    suspend fun byId(collectionId: String): Mono<FlowNftCollectionDto> = repo.findById(collectionId)
        .map { item -> FlowNftCollectionDto(
            id = item.id,
            owner = item.owner.formatted,
            name = item.name,
            symbol = item.symbol
        ) }
}
