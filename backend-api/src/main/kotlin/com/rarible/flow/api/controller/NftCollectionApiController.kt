package com.rarible.flow.api.controller

import com.rarible.flow.api.service.CollectionService
import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftCollectionControllerApi
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class NftCollectionApiController(
    private val service: CollectionService
): FlowNftCollectionControllerApi {
    override suspend fun getNftCollectionById(collection: String): ResponseEntity<FlowNftCollectionDto> =
        ResponseEntity.ok(service.byId(collection).awaitSingle())
}
