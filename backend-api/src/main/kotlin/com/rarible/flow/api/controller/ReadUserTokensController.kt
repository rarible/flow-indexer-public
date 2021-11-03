package com.rarible.flow.api.controller

import com.rarible.flow.api.service.UserStorageService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class ReadUserTokensController(private val userStorageService: UserStorageService) {

    @GetMapping("/v0.1/newLogin/{address}")
    suspend fun newLogin(@PathVariable address: String) {
        val flowAddress = address.flowAddress()!!
        userStorageService.scanNFT(flowAddress)
    }
}
