package com.rarible.flow.api.controller

import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.randomAddress
import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
                collection,
                collection.copy(id = "ID2", owner = FlowAddress("0x01")),
                collection.copy(id = "ID4", owner = FlowAddress("0x02"))
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

        collection.id shouldBe "ID4"
    }

    @Test
    fun `should return all collections`() {
        val response = client.get()
            .uri("/v0.1/collections/all")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowNftCollectionsDto::class.java)
            .returnResult().responseBody!!

        response.total shouldBe 3
    }

    @Test
    fun `should return collections by owner`() {
        var response = client.get()
            .uri("/v0.1/collections/byOwner?owner=0x01")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowNftCollectionsDto::class.java)
            .returnResult().responseBody!!

        response.total shouldBe 1
        response.data[0].owner shouldBe "0x01"

        response = client.get()
            .uri("/v0.1/collections/byOwner?owner=0x02")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowNftCollectionsDto::class.java)
            .returnResult().responseBody!!

        response.total shouldBe 1
        response.data[0].owner shouldBe "0x02"
    }
}
