package com.rarible.flow.api.controller

import com.rarible.flow.api.service.BlockInfoService
import com.rarible.flow.core.block.ServiceBlockInfo
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class ServiceBlocksInfoController(
    private val service: BlockInfoService
) {

    @GetMapping("v0.1/service/status")
    suspend fun blockInfo(): ServiceBlockInfo  = service.info()
}
