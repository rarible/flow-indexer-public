package com.rarible.flow.api.controller

import com.rarible.flow.api.service.ActivitiesService
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOrderActivityControllerApi
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class NftOrderActivityController(private val service: ActivitiesService): FlowNftOrderActivityControllerApi {
    override suspend fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowActivitiesDto> =
        ResponseEntity.ok(service.getNftOrderActivitiesByItem(type, contract, tokenId, continuation, size).awaitFirst())

    override suspend fun getNftOrderActivitiesByUser(
        type: List<String>,
        user: List<String>,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowActivitiesDto> =
        ResponseEntity.ok(service.getNftOrderActivitiesByUser(type, user, continuation, size).awaitFirst())

    override suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowActivitiesDto> =
        ResponseEntity.ok(service.getNftOrderAllActivities(type, continuation, size).awaitFirst())
}
