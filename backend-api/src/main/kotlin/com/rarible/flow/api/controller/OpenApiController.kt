package com.rarible.flow.api.controller

import org.springframework.core.io.InputStreamResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v0.1"])
class OpenApiController() {

    @GetMapping(
        value = ["/openapi.yaml"],
        produces = ["text/yaml"]
    )
    fun openapiYaml(): InputStreamResource {
        return InputStreamResource(
            OpenApiController::class.java.getResourceAsStream("/nft-api.yaml")
        )
    }

    @GetMapping(
        value = ["/doc"],
        produces = ["text/html"]
    )
    fun doc(): InputStreamResource {
        val file = OpenApiController::class.java.getResourceAsStream("/redoc.html")
        return InputStreamResource(file)
    }
}