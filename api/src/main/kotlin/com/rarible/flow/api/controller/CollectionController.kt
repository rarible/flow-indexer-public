package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.service.CollectionService
import com.rarible.flow.api.util.okOr404IfNull
import com.rarible.flow.core.converter.FlowNftCollectionDtoConverter
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.CollectionFilter
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
class CollectionController(
    private val service: CollectionService
) : FlowNftCollectionControllerApi {

    override suspend fun getNftCollectionById(collection: String): ResponseEntity<FlowNftCollectionDto> =
        convert(service.byId(collection)).okOr404IfNull()

    override suspend fun searchNftAllCollections(
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftCollectionsDto> {
        return convert(
            service.searchAll(continuation, size), size
        ).okOr404IfNull()
    }

    override suspend fun searchNftCollectionsByIds(ids: List<String>): ResponseEntity<FlowNftCollectionsDto> {
        val collections = service.byIds(ids).toList()
        return ResponseEntity.ok(
            FlowNftCollectionsDto(
                data = collections.mapNotNull { convert(it) },
                continuation = null
            )
        )
    }

    override suspend fun searchNftCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftCollectionsDto> {
        return convert(
            service
                .searchByOwner(FlowAddress(owner), continuation, size),
            size
        ).okOr404IfNull()
    }

    private fun convert(collection: ItemCollection?) = collection?.let {
        FlowNftCollectionDtoConverter.convert(it)
    }

    private suspend fun convert(collections: Flow<ItemCollection>, size: Int?): FlowNftCollectionsDto {
        val data: List<ItemCollection> = collections.toList()
        return if (data.isEmpty()) {
            FlowNftCollectionsDto(null, emptyList())
        } else {
            FlowNftCollectionsDto(
                CollectionFilter.Sort.BY_ID.nextPage(collections, size),
                data.map { convert(it)!! }
            )
        }
    }
}
