package com.rarible.flow.api.controller

import com.rarible.flow.api.service.ActivitiesService
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.NftActivitiesByIdRequestDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOrderActivityControllerApi
import java.time.Instant
import kotlinx.coroutines.FlowPreview
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@FlowPreview
@RestController
@CrossOrigin
class NftOrderActivityController(private val service: ActivitiesService) : FlowNftOrderActivityControllerApi {

    companion object {
        const val defaultSort = "LATEST_FIRST"
    }

    override suspend fun getNftOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> =
        ResponseEntity.ok(
            service.getNftOrderActivitiesByCollection(type, collection, continuation, size, sort ?: defaultSort)
        )

    override suspend fun getNftOrderActivitiesById(nftActivitiesByIdRequestDto: NftActivitiesByIdRequestDto): ResponseEntity<FlowActivitiesDto> {
        return ResponseEntity.ok(
            service.getActivitiesByIds(nftActivitiesByIdRequestDto.ids)
        )
    }

    override suspend fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> {
        return ResponseEntity.ok(
            service.getNftOrderActivitiesByItem(type, contract, tokenId, continuation, size, sort ?: defaultSort)
        )
    }

    override suspend fun getNftOrderActivitiesByUser(
        type: List<String>,
        user: List<String>,
        from: Long?,
        to: Long?,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> = ResponseEntity.ok(
        service.getNftOrderActivitiesByUser(
            type,
            user,
            continuation,
            from?.let { Instant.ofEpochMilli(from) },
            to?.let { Instant.ofEpochMilli(to) },
            size,
            sort ?: defaultSort)
    )

    override suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> =
        ResponseEntity.ok(
            service.getNftOrderAllActivities(type.filterNot { it.isEmpty() }, continuation, size, sort ?: defaultSort)
        )
}
