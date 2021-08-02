package com.rarible.flow.api.controller

import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.randomAddress
import com.rarible.protocol.dto.FlowNftCollectionDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient(timeout = "60000")
@ActiveProfiles("test")
class NftCollectionApiControllerTest {

    @Autowired
    private lateinit var repo: ItemCollectionRepository

    @Autowired
    private lateinit var client: WebTestClient


    @BeforeEach
    internal fun setUp() {
        repo.deleteAll().block()
        val collection = ItemCollection(
            id = "ID1",
            owner = FlowAddress(randomAddress()),
            name = "Test Collection 1",
            symbol = "TC1"
        )

        repo.saveAll(
            listOf(
                collection, collection.copy(id = "ID2"), collection.copy(id = "ID4")
            )
        ).then().block()
    }

    @Test
    fun `should return collection by id`() {
        val collection = client.get()
            .uri("/v0.1/collections/ID4")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowNftCollectionDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(collection)
        Assertions.assertEquals("ID4", collection.id, "bad ID")
    }
}
