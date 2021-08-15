package com.rarible.flow.api.controller

import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.randomAddress
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import kotlin.random.Random

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
class OwnershipsApiControllerTest {

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var client: WebTestClient

    @BeforeEach
    internal fun setUp() {
        ownershipRepository.deleteAll().block()
    }

    @Test
    internal fun `should return ownership by id`() {
        val contract = FlowAddress(randomAddress())
        val owner = FlowAddress(randomAddress())
        val tokenId = Random.Default.nextLong(0L, Long.MAX_VALUE)
        val ownership = Ownership(
            contract = contract,
            tokenId = tokenId,
            owner = owner,
            date = Instant.now(),
            creators = listOf(Payout(account = FlowAddress(randomAddress()), value = BigDecimal.ONE))
        )

        ownershipRepository.save(ownership).block()

        client.get()
            .uri("/v0.1/ownerships/{ownershipId}", mapOf("ownershipId" to ownership.id.toString()))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftOwnershipDto>()
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                val ownershipDto = it.responseBody!!
                Assertions.assertEquals(contract.formatted, ownershipDto.token, "Token is not equals!")
                Assertions.assertEquals(owner.formatted, ownershipDto.owner, "Owner is not equals!")
                Assertions.assertEquals(tokenId, ownershipDto.tokenId.toLong(), "Token ID is not equals!")
                Assertions.assertNotNull(ownershipDto.creators)
                Assertions.assertTrue(ownershipDto.creators?.isNotEmpty() ?: false)

            }
    }

    @Test
    internal fun `should return all ownerships`() {
        ownershipRepository.saveAll(
            listOf(
                Ownership(
                    contract = FlowAddress(randomAddress()),
                    tokenId = randomLong(),
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now(),
                ),
                Ownership(
                    contract = FlowAddress(randomAddress()),
                    tokenId = randomLong(),
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now()
                ),
                Ownership(
                    contract = FlowAddress(randomAddress()),
                    tokenId = randomLong(),
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now()
                ),
            )
        ).collectList().block()

        client.get().uri("/v0.1/ownerships/all")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftOwnershipsDto>()
            .consumeWith {
                val list = it.responseBody!!
                Assertions.assertTrue(list.ownerships.isNotEmpty())
                Assertions.assertTrue(list.ownerships.size == 3)
            }

    }


    @Test
    internal fun `should return all ownerships by item`() {
        val tokenId = randomLong()
        val contract = FlowAddress(randomAddress())

        ownershipRepository.saveAll(
            listOf(
                Ownership(
                    contract = contract,
                    tokenId = tokenId,
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now(Clock.systemUTC())
                ),
                Ownership(
                    contract = contract,
                    tokenId = tokenId,
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now(Clock.systemUTC())
                ),
                Ownership(
                    contract = FlowAddress(randomAddress()),
                    tokenId = tokenId,
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now(Clock.systemUTC())
                ),
                Ownership(
                    contract = contract,
                    tokenId = randomLong(),
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now(Clock.systemUTC())
                ),

            )
        ).collectList().block()

        client.get().uri("/v0.1/ownerships/byItem?contract={contract}&tokenId={tokenId}", mapOf("contract" to contract.formatted, "tokenId" to tokenId))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftOwnershipsDto>()
            .consumeWith {
                Assertions.assertNotNull(it.responseBody?.ownerships)
                val response = it.responseBody!!
                Assertions.assertTrue(response.ownerships.isNotEmpty())
                Assertions.assertTrue(response.ownerships.size == 2)
                response.ownerships.forEach {
                    Assertions.assertEquals(contract.formatted, it.token)
                    Assertions.assertEquals(tokenId, it.tokenId.toLong())
                }
            }
    }


}
