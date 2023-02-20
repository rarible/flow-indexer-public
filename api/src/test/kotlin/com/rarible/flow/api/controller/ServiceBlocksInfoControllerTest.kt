package com.rarible.flow.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.service.BlockInfoService
import com.rarible.flow.core.block.BlockInfo
import com.rarible.flow.core.block.ServiceBlockInfo
import io.mockk.coEvery
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.time.temporal.ChronoUnit

@WebFluxTest(
    controllers = [ServiceBlocksInfoController::class],
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "spring.data.mongodb.auto-index-creation = true"
    ]
)
@ActiveProfiles("test")
class ServiceBlocksInfoControllerTest(
    @Autowired private val client: WebTestClient,
) {

    @MockkBean
    private lateinit var service: BlockInfoService

    @BeforeEach
    internal fun setUp() {
        coEvery {
            service.info()
        } returns ServiceBlockInfo(
            lastBlockInBlockchain = BlockInfo(
                blockHeight = 234567891L,
                timestamp = Instant.now().toEpochMilli()
            ),
            lastBlockInIndexer = BlockInfo(
                blockHeight = 123456789L,
                timestamp = Instant.now().minus(1L, ChronoUnit.MINUTES).toEpochMilli()
            )
        )
    }

    @Test
    internal fun infoTest() {
        client.get().uri("/v0.1/service/status")
            .exchange()
            .expectStatus().isOk
            .expectBody<ServiceBlockInfo>()
            .consumeWith {
                val data = it.responseBody
                Assertions.assertNotNull(data)
                Assertions.assertTrue(data!!.lastBlockInBlockchain.blockHeight > data.lastBlockInIndexer!!.blockHeight)
                Assertions.assertTrue(data.lastBlockInBlockchain.timestamp > data.lastBlockInIndexer!!.timestamp)
            }
    }
}
