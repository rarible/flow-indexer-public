package com.rarible.flow.api.controller

import com.rarible.flow.api.service.OwnershipsService
import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.repository.OwnershipFilter
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOwnershipControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
class OwnershipsApiController(
    private val service: OwnershipsService
) : FlowNftOwnershipControllerApi {

    override suspend fun getNftAllOwnerships(continuation: String?, size: Int?): ResponseEntity<FlowNftOwnershipsDto> {
        val sort = OwnershipFilter.Sort.LATEST_FIRST
        val ownerships = service.all(continuation, size, sort)
        return convert(ownerships, size, sort).okOr404IfNull()
    }

    override suspend fun getNftOwnershipById(ownershipId: String): ResponseEntity<FlowNftOwnershipDto> {
        val id = OwnershipId.parse(ownershipId)
        return service
            .byId(id)
            ?.let { o -> OwnershipToDtoConverter.convert(o) }
            .okOr404IfNull()
    }

    override suspend fun getNftOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftOwnershipsDto> {
        val itemId = ItemId(contract, tokenId.tokenId())
        val sort = OwnershipFilter.Sort.LATEST_FIRST
        val ownerships = service.byItem(itemId, continuation, size, sort)
        return convert(ownerships, size, sort).okOr404IfNull()
    }


    private suspend fun convert(
        flow: Flow<Ownership>,
        size: Int?,
        sort: OwnershipFilter.Sort
    ): FlowNftOwnershipsDto {
        return FlowNftOwnershipsDto(
            total = flow.count().toLong(),
            ownerships = flow.map { o -> OwnershipToDtoConverter.convert(o) }.toList(),
            continuation = sort.nextPage(flow, size)
        )
    }
}
