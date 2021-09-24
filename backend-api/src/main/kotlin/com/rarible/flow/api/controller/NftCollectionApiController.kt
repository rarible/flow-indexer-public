package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.service.CollectionService
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.NftCollectionContinuation
import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftCollectionControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class NftCollectionApiController(
    private val service: CollectionService
): FlowNftCollectionControllerApi {
    override suspend fun getNftCollectionById(collection: String): ResponseEntity<FlowNftCollectionDto> =
        convert(service.byId(collection)).okOr404IfNull()

    override suspend fun searchNftAllCollections(
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftCollectionsDto> {
        return convert(
            service
            .searchAll(NftCollectionContinuation.parse(continuation), size)
        ).okOr404IfNull()
    }

    override suspend fun searchNftCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftCollectionsDto> {
        return convert(
            service
                .searchByOwner(FlowAddress(owner), NftCollectionContinuation.parse(continuation), size)
        ).okOr404IfNull()
    }


    private fun convert(collection: ItemCollection?) = collection?.let {
        FlowNftCollectionDto(
            id = it.id,
            owner = it.owner.formatted,
            name = it.name,
            symbol = it.symbol
        )
    }

    private suspend fun convert(collections: Flow<ItemCollection>): FlowNftCollectionsDto {
        val data: List<ItemCollection> = collections.toList()
        return if(data.isEmpty()) {
            FlowNftCollectionsDto(0, null, emptyList())
        } else {
            val last = data.last()
            FlowNftCollectionsDto(
                data.size.toLong(),
                NftCollectionContinuation(last.createdDate, last.id).toString(),
                data.map { convert(it)!! }
            )
        }

    }
}
