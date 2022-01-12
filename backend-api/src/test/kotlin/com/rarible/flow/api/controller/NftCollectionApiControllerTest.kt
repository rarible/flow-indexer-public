package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.service.CollectionService
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.randomAddress
import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import kotlinx.coroutines.flow.asFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(
    controllers = [NftCollectionApiController::class],
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "blockchain.scanner.flow.chainId = TESTNET"
    ]
)
@ActiveProfiles("test")
class NftCollectionApiControllerTest(
    @Autowired val client: WebTestClient
) {

    @MockkBean
    private lateinit var service: CollectionService

    @BeforeEach
    internal fun setUp() {
        val collection = ItemCollection(
            id = "ID1",
            owner = FlowAddress(randomAddress()),
            name = "Test Collection 1",
            symbol = "TC1"
        )

        val collection2 = collection.copy(id = "ID2", owner = FlowAddress("0x01"))

        val collection3 = collection.copy(id = "ID4", owner = FlowAddress("0x02"))

        val all = listOf(
            collection,
            collection2,
            collection3
        )

        coEvery {
            service.byId(eq("ID1"))
        } returns collection

        coEvery {
            service.byId(eq("ID2"))
        } returns collection2

        coEvery {
            service.byId(eq("ID4"))
        } returns collection3

        coEvery {
            service.searchAll(any(), any())
        } returns all.asFlow()

        coEvery {
            service.searchByOwner(eq(FlowAddress("0x01")), any(), any())
        } returns listOf(collection2).asFlow()

        coEvery {
            service.searchByOwner(eq(FlowAddress("0x02")), any(), any())
        } returns listOf(collection3).asFlow()
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
        response.data[0].owner shouldBe FlowAddress("0x01").formatted

        response = client.get()
            .uri("/v0.1/collections/byOwner?owner=0x02")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowNftCollectionsDto::class.java)
            .returnResult().responseBody!!

        response.total shouldBe 1
        response.data[0].owner shouldBe FlowAddress("0x02").formatted
    }
}
