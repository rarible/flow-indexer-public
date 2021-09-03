package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.randomAddress
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import io.kotest.matchers.collections.shouldNotHaveSize
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

@InternalCoroutinesApi
@SpringBootTest(
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "spring.data.mongodb.auto-index-creation = true"
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient(timeout = "60000")
@MongoTest
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
        runBlocking {
            val contract = randomAddress()
            val owner = FlowAddress(randomAddress())
            val tokenId = Random.Default.nextLong(0L, Long.MAX_VALUE)
            val ownership = Ownership(
                contract = contract,
                tokenId = tokenId,
                owner = owner,
                date = Instant.now(Clock.systemUTC()),
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
                    Assertions.assertEquals(contract, ownershipDto.contract, "Token is not equals!")
                    Assertions.assertEquals(owner.formatted, ownershipDto.owner, "Owner is not equals!")
                    Assertions.assertEquals(tokenId, ownershipDto.tokenId.toLong(), "Token ID is not equals!")
                    Assertions.assertNotNull(ownershipDto.creators)
                    ownershipDto.creators shouldNotHaveSize 0

                }
        }
    }

    @Test
    internal fun `should return all ownerships`() {
        runBlocking {
            ownershipRepository.saveAll(
                listOf(
                    Ownership(
                        contract = randomAddress(),
                        tokenId = randomLong(),
                        owner = FlowAddress(randomAddress()),
                        date = Instant.now(Clock.systemUTC()),
                    ),
                    Ownership(
                        contract = randomAddress(),
                        tokenId = randomLong(),
                        owner = FlowAddress(randomAddress()),
                        date = Instant.now(Clock.systemUTC())
                    ),
                    Ownership(
                        contract = randomAddress(),
                        tokenId = randomLong(),
                        owner = FlowAddress(randomAddress()),
                        date = Instant.now(Clock.systemUTC())
                    ),
                )
            ).then().block()

            client.get().uri("/v0.1/ownerships/all")
                .exchange()
                .expectStatus().isOk
                .expectBody<FlowNftOwnershipsDto>()
                .consumeWith {
                    val list = it.responseBody!!
                    Assertions.assertTrue(list.ownerships.isNotEmpty())
                    Assertions.assertTrue(list.ownerships.size == 3)
                    Assertions.assertNotNull(list.continuation)
                    Assertions.assertNotNull(list.total)
                    Assertions.assertEquals(list.total, list.ownerships.size)
                }
        }

    }


    @Test
    internal fun `should return all ownerships by item`() {
        runBlocking {
            val tokenId = randomLong()
            val contract = randomAddress()

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
                        contract = randomAddress(),
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
            ).then().block()

            client.get().uri(
                "/v0.1/ownerships/byItem?contract={contract}&tokenId={tokenId}",
                mapOf("contract" to contract, "tokenId" to tokenId)
            )
                .exchange()
                .expectStatus().isOk
                .expectBody<FlowNftOwnershipsDto>()
                .consumeWith {
                    Assertions.assertNotNull(it.responseBody?.ownerships)
                    val response = it.responseBody!!
                    Assertions.assertTrue(response.ownerships.isNotEmpty())
                    Assertions.assertTrue(response.ownerships.size == 2)
                    response.ownerships.forEach { o ->
                        Assertions.assertEquals(contract, o.contract)
                        Assertions.assertEquals(tokenId, o.tokenId.toLong())
                    }
                }
        }
    }

    @Test
    internal fun `ownerships continuation test`() {
        runBlocking {
            var tokenId = randomLong()
            val contract = randomAddress()

            val ownerships = listOf(
                Ownership(
                    contract = contract,
                    tokenId = ++tokenId,
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now(Clock.systemUTC()).plusSeconds(1L)
                ),
                Ownership(
                    contract = contract,
                    tokenId = ++tokenId,
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now(Clock.systemUTC()).plusSeconds(2L)
                ),
                Ownership(
                    contract = randomAddress(),
                    tokenId = ++tokenId,
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now(Clock.systemUTC()).plusSeconds(3L)
                ),
                Ownership(
                    contract = contract,
                    tokenId = ++tokenId,
                    owner = FlowAddress(randomAddress()),
                    date = Instant.now(Clock.systemUTC()).plusSeconds(5L)
                ),
            )

            ownershipRepository.saveAll(
                ownerships
            ).then().block()

            val allOwnerships = client.get().uri("/v0.1/ownerships/all")
                .exchange()
                .expectStatus().isOk
                .expectBody<FlowNftOwnershipsDto>()
                .consumeWith { response ->
                    Assertions.assertNotNull(response.responseBody)
                    val ownershipsDto = response.responseBody!!
                    Assertions.assertNotNull(ownershipsDto.continuation)
                    Assertions.assertNotNull(ownershipsDto.total)
                    Assertions.assertEquals(ownershipsDto.total, ownershipsDto.ownerships.size)
                }.returnResult().responseBody!!

            client.get().uri("/v0.1/ownerships/all?size=1")
                .exchange()
                .expectStatus().isOk
                .expectBody<FlowNftOwnershipsDto>()
                .consumeWith { response ->
                    Assertions.assertNotNull(response.responseBody)
                    val oneOwnershipsDto = response.responseBody!!
                    Assertions.assertNotNull(oneOwnershipsDto.continuation)
                    Assertions.assertNotNull(oneOwnershipsDto.total)
                    Assertions.assertEquals(oneOwnershipsDto.total, oneOwnershipsDto.ownerships.size)
                    Assertions.assertEquals(allOwnerships.ownerships[0].id, oneOwnershipsDto.ownerships[0].id)

                    client.get().uri(
                        "/v0.1/ownerships/all?size=1&continuation={continuation}",
                        mapOf("continuation" to oneOwnershipsDto.continuation)
                    )
                        .exchange().expectStatus().isOk
                        .expectBody<FlowNftOwnershipsDto>()
                        .consumeWith { nextResponse ->
                            Assertions.assertNotNull(nextResponse.responseBody)
                            val nextOwnershipsDto = nextResponse.responseBody!!
                            Assertions.assertNotNull(nextOwnershipsDto.continuation)
                            Assertions.assertNotNull(nextOwnershipsDto.total)
                            Assertions.assertEquals(nextOwnershipsDto.total, nextOwnershipsDto.ownerships.size)

                            Assertions.assertEquals(allOwnerships.ownerships[1].id, nextOwnershipsDto.ownerships[0].id)
                        }

                    client.get().uri(
                        "/v0.1/ownerships/all?size=2&continuation={continuation}",
                        mapOf("continuation" to oneOwnershipsDto.continuation)
                    )
                        .exchange().expectStatus().isOk
                        .expectBody<FlowNftOwnershipsDto>()
                        .consumeWith { nextResponse ->
                            Assertions.assertNotNull(nextResponse.responseBody)
                            val nextOwnershipsDto = nextResponse.responseBody!!
                            Assertions.assertNotNull(nextOwnershipsDto.continuation)
                            Assertions.assertNotNull(nextOwnershipsDto.total)
                            Assertions.assertEquals(nextOwnershipsDto.total, nextOwnershipsDto.ownerships.size)

                            Assertions.assertEquals(allOwnerships.ownerships[1].id, nextOwnershipsDto.ownerships[0].id)
                            Assertions.assertEquals(allOwnerships.ownerships[2].id, nextOwnershipsDto.ownerships[1].id)
                        }
                }
        }
    }
}
