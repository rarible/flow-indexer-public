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
import java.time.Instant

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

    override suspend fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> {
        val types = safeOf(type, FlowActivityType.values().toList())
        val cont = ActivityContinuation.of(continuation)
        return ResponseEntity.ok(
            service.getNftOrderActivitiesByItem(types, contract, tokenId, cont, size, sort ?: defaultSort)
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
    ): ResponseEntity<FlowActivitiesDto> {
        val fromInstant = from?.let { Instant.ofEpochMilli(from) }
        val toInstant = to?.let { Instant.ofEpochMilli(to) }
        return ResponseEntity.ok(
            service.getNftOrderActivitiesByUser(type, user, continuation, fromInstant, toInstant, size, sort ?: defaultSort)
        )
    }

    override suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> =
        ResponseEntity.ok(
            service.getNftOrderAllActivities(type, continuation, size, sort ?: defaultSort)
        )
}
