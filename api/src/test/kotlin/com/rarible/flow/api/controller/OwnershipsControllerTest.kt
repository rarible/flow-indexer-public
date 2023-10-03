package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.service.OwnershipService
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.randomAddress
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import kotlinx.coroutines.flow.asFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Clock
import java.time.Instant
import kotlin.random.Random

@WebFluxTest(
    controllers = [OwnershipController::class],
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
class OwnershipsControllerTest(
    @Autowired val client: WebTestClient
) {

    @MockkBean
    private lateinit var service: OwnershipService

    val contract = randomAddress()
    val owner = FlowAddress(randomAddress())
    val tokenId = Random.Default.nextLong(0L, Long.MAX_VALUE)

    val ownership1 = Ownership(
        contract = contract,
        tokenId = tokenId,
        owner = owner,
        date = Instant.now(Clock.systemUTC()),
        creator = FlowAddress(randomAddress())
    )

    val ownership2 = ownership1.copy(tokenId = tokenId, owner = FlowAddress(randomAddress()))
    val ownership3 = ownership2.copy(tokenId = tokenId + 1)
    val all = listOf(ownership1, ownership2, ownership3)

    @BeforeEach
    fun setUp() {
        coEvery {
            service.byId(eq(OwnershipId(contract, tokenId, owner)))
        } returns ownership1

        coEvery {
            service.all(any(), any(), any())
        } returns all.asFlow()

        coEvery {
            service.byItem(eq(ItemId(contract, tokenId)), any(), any(), any())
        } returns listOf(ownership1, ownership2).asFlow()
    }

    @Test
    internal fun `should return ownership by id`() {
        client.get()
            .uri("/v0.1/ownerships/{ownershipId}", mapOf("ownershipId" to ownership1.id.toString()))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftOwnershipDto>()
            .consumeWith {
                it.responseBody shouldNotBe null
                val ownershipDto = it.responseBody!!
                ownershipDto.contract shouldBe contract
                ownershipDto.owner shouldBe owner.formatted
                ownershipDto.tokenId shouldBe tokenId.toBigInteger()
            }
    }

    @Test
    internal fun `should return all ownerships`() {
        client
            .get()
            .uri("/v0.1/ownerships/all")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftOwnershipsDto>()
            .consumeWith {
                val list = it.responseBody!!
                list.ownerships shouldHaveSize 3
            }
    }

    @Test
    internal fun `should return all ownerships by item`() {
        client
            .get()
            .uri(
                "/v0.1/ownerships/byItem?contract={contract}&tokenId={tokenId}",
                mapOf("contract" to contract, "tokenId" to tokenId)
            )
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftOwnershipsDto>()
            .consumeWith {
                val ownerships = it.responseBody!!.ownerships
                ownerships shouldHaveSize 2
            }
    }
}
