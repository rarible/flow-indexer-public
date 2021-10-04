package com.rarible.flow.api.controller

import com.rarible.flow.api.service.ActivitiesService
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.enum.safeOf
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOrderActivityControllerApi
import kotlinx.coroutines.FlowPreview
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@FlowPreview
@RestController
@CrossOrigin
class NftOrderActivityController(private val service: ActivitiesService): FlowNftOrderActivityControllerApi {

    override suspend fun getNftOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowActivitiesDto> =
        ResponseEntity.ok(service.getNfdOrderActivitiesByCollection(type, collection, continuation, size))

    override suspend fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowActivitiesDto> {
        val types = safeOf(type, FlowActivityType.values().toList())
        val cont = ActivityContinuation.of(continuation)
        return ResponseEntity.ok(
            service.getNftOrderActivitiesByItem(types, contract, tokenId, cont, size)
        )
    }

    override suspend fun getNftOrderActivitiesByUser(
        type: List<String>,
        user: List<String>,
        continuation: String?,
        size: Int?,
        sort: String?
    ): ResponseEntity<FlowActivitiesDto> =
        ResponseEntity.ok(service.getNftOrderActivitiesByUser(type, user, continuation, size, sort))

    override suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowActivitiesDto> =
        ResponseEntity.ok(service.getNftOrderAllActivities(type, continuation, size))
}
