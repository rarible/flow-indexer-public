package com.rarible.flow.api.controller

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@WebFluxTest(
    controllers = [OpenApiController::class],
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ActiveProfiles("test")
internal class OpenApiControllerTest(
    @Autowired val client: WebTestClient
) {

    @Test
    fun `should return open api yaml`() {
        client
            .get()
            .uri("/v0.1/openapi.yaml")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>().consumeWith {
                it.responseBody shouldContain "title: \"flow-protocol-api-nft\""
            }
    }

    @Test
    fun `should return redoc`() {
        client
            .get()
            .uri("/v0.1/doc")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>().consumeWith {
                it.responseBody shouldContain "<redoc spec-url='/v0.1/openapi.yaml'></redoc>"
            }
    }
}
