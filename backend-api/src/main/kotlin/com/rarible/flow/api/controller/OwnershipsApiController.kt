package com.rarible.flow.api.controller

import com.rarible.flow.api.service.OwnershipsService
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOwnershipControllerApi
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.onflow.sdk.FlowAddress
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
class OwnershipsApiController(
    private val service: OwnershipsService
) : FlowNftOwnershipControllerApi {

    override suspend fun getNftAllOwnerships(continuation: String?, size: Int?): ResponseEntity<FlowNftOwnershipsDto> =
        ResponseEntity.ok(service.all(continuation, size).awaitSingle())

    override suspend fun getNftOwnershipById(ownershipId: String): ResponseEntity<FlowNftOwnershipDto> =
        ResponseEntity.ok(service.byId(id = ownershipId).awaitSingleOrNull())

    override suspend fun getNftOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<FlowNftOwnershipsDto> =
        ResponseEntity.ok(service.byItem(FlowAddress(contract), tokenId.toLong(), continuation, size).awaitSingle())
}
