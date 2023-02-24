package com.rarible.flow.api.controller

import com.rarible.flow.api.service.ActivityService
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.NftActivitiesByIdRequestDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOrderActivityControllerApi
import kotlinx.coroutines.FlowPreview
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@FlowPreview
@RestController
@CrossOrigin
class ActivityController(private val service: ActivityService) : FlowNftOrderActivityControllerApi {

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

    override suspend fun getNftOrderActivitiesByCollections(
        type: List<String>,
        collection: List<String>,
        continuation: String?,
        size: Int?,
        sort: String?
    ): ResponseEntity<FlowActivitiesDto> {
        return ResponseEntity.ok(
            service.getNftOrderActivitiesByCollections(type, collection, continuation, size, sort ?: defaultSort)
        )
    }

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

    override suspend fun getNftOrderActivitiesByItemAndOwner(
        type: List<String>,
        contract: String,
        tokenId: Long,
        owner: String,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> {
        return ResponseEntity.ok(
            service.getNftOrderActivitiesByItemAndOwner(type, contract, tokenId, owner, continuation, size, sort ?: defaultSort)
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

    override suspend fun getNftOrderActivitiesSync(
        type: List<String>,
        continuation: String?,
        size: Int?,
        sort: String?
    ): ResponseEntity<FlowActivitiesDto> = ResponseEntity.ok(
        service.syncActivities(type, size, continuation, sort ?: defaultSort)
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
