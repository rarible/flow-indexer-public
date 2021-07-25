package com.rarible.flow.api.controller

import com.rarible.protocol.flow.nft.api.controller.FlowNftOrderActivityControllerApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class NftOrderActivityController: FlowNftOrderActivityControllerApi {
    override suspend fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?
    ): ResponseEntity<com.rarible.protocol.dto.ActivitiesDto> {
        TODO("Not yet implemented")
    }
}
