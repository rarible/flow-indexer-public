package com.rarible.flow.api.controller

import com.mongodb.client.result.UpdateResult
import com.nftco.flow.sdk.FlowAddress
import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.TestPropertiesConfiguration
import com.rarible.flow.api.http.shouldGetBadRequest
import com.rarible.flow.api.http.shouldGetPaginatedResult
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.randomFlowAddress
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@WebFluxTest(
    controllers = [TaskController::class],
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@AutoConfigureWebTestClient(timeout = "60000")
@ActiveProfiles("test")
@Import(TestPropertiesConfiguration::class)
class TaskControllerTest {

    @Autowired lateinit var client: WebTestClient
    @MockkBean lateinit var mongoTemplate: ReactiveMongoTemplate

    @Test
    fun `should update task by id`() {
        coEvery {
            mongoTemplate.updateFirst(
                any(),
                any(),
                "task"
            )
        } returns UpdateResult.acknowledged(1, 1L, null).toMono()

        val result = client.post()
            .uri(
                "/v0.1/task/620f7b3a0aa2d233ccafdf3e?state=1"
            )
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult().responseBody!!

        result shouldBe "ok"
    }
}
