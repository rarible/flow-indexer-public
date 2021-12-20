package com.rarible.flow.api.controller

import com.rarible.flow.api.service.UserStorageService
import com.rarible.flow.log.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@DelicateCoroutinesApi
@RestController
@CrossOrigin
class ReadUserTokensController(private val userStorageService: UserStorageService) {

    @GetMapping("/v0.1/newLogin/{address}")
    suspend fun newLogin(@PathVariable address: String): String {
        val flowAddress = address.flowAddress()!!
        GlobalScope
            .launch { userStorageService.scanNFT(flowAddress) }
            .invokeOnCompletion { completionCause ->
                if(completionCause == null) {
                    logger.info("New login for {} completed", address)
                } else {
                    logger.error("New login error", completionCause)
                }
            }
        return "OK"
    }

    companion object {
        val logger by Log()
    }
}
