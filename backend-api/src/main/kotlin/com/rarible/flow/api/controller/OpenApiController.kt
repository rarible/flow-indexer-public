package com.rarible.flow.api.controller

import com.rarible.protocol.flow.api.FlowOpenapiReader
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
    fun openapiYaml(): InputStreamResource = InputStreamResource(FlowOpenapiReader.getOpenapi())


    @GetMapping(
        value = ["/doc"],
        produces = ["text/html"]
    )
    fun doc(): InputStreamResource = InputStreamResource(OpenApiController::class.java.getResourceAsStream("/redoc.html")!!)

}
