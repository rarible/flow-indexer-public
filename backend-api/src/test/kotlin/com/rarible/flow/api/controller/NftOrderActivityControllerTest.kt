package com.rarible.flow.api.controller

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.api.config.Config
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.randomAddress
import com.rarible.flow.randomFlowAddress
import com.rarible.flow.randomLong
import com.rarible.flow.randomRate
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowBurnDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.testcontainers.shaded.org.apache.commons.lang.math.RandomUtils
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
@MongoTest
@ActiveProfiles("test")
@Import(Config::class, CoreConfig::class)
class NftOrderActivityControllerTest {

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var repo: ItemHistoryRepository

    @Autowired
    lateinit var client: WebTestClient

    @BeforeEach
    internal fun setUp() {
        repo.deleteAll().block()
    }

    @Test
    internal fun `should handle basic request item history by user`() {
        val expectedUser = randomAddress()

        val history = listOf(
            randomItemHistory(activity = randomMint().copy(owner = expectedUser)),
            randomItemHistory(activity = randomBurn().copy(owner = expectedUser)),
        )
        repo.saveAll(history).then().block()
        client.get()
            .uri("/v0.1/order/activities/byUser?type=MINT&user=$expectedUser")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowActivitiesDto>()
            .consumeWith { response ->
                val activitiesDto = response.responseBody
                Assertions.assertNotNull(activitiesDto)
                Assertions.assertNotNull(activitiesDto?.items)
                Assertions.assertEquals(1, activitiesDto?.items?.size)
            }
    }

    @Test
    internal fun `should work burn by user`() {
        val expectedUser = "0x01658d9b94068f3c"
        val tokenId = randomLong()
        val date = Instant.now(Clock.systemUTC())
        val hash = "12345"

        repo.saveAll(listOf(
            randomItemHistory(date = date, activity = randomWithdraw().copy(tokenId = tokenId, from = expectedUser), log = randomLog().copy(transactionHash = hash)),
            randomItemHistory(date = date, activity = randomBurn().copy(tokenId = tokenId, owner = null), log = randomLog().copy(transactionHash = hash)),
        )
        ).then().block()

        client.get()
            .uri("/v0.1/order/activities/byUser?type=BURN&user=$expectedUser")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowActivitiesDto>()
            .consumeWith { response ->
                val activitiesDto = response.responseBody
                Assertions.assertNotNull(activitiesDto)
                Assertions.assertNotNull(activitiesDto?.items)
                Assertions.assertEquals(1, activitiesDto?.items?.size)
                activitiesDto?.items?.firstOrNull()?.apply {
                    Assertions.assertEquals(expectedUser, (this as FlowBurnDto).owner)
                }
            }
    }

    private fun randomMint() = MintActivity(
        type = FlowActivityType.MINT,
        timestamp = Instant.now(Clock.systemUTC()),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomLong(),
        value = RandomUtils.nextLong(),
        metadata = mapOf("metaURI" to "ipfs://"),
        royalties = (0..Random.Default.nextInt(0, 3)).map { Part(randomFlowAddress(), randomRate()) },
    )

    fun randomBurn() = BurnActivity(
        type = FlowActivityType.BURN,
        timestamp = Instant.now(Clock.systemUTC()),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomLong(),
        value = RandomUtils.nextLong(),
    )

    fun randomWithdraw() = WithdrawnActivity(
        type = FlowActivityType.BURN,
        timestamp = Instant.now(Clock.systemUTC()),
        from = randomAddress(),
        contract = randomAddress(),
        tokenId = randomLong(),
    )

    fun randomItemHistory(
        date: Instant = Instant.now(Clock.systemUTC()),
        activity: BaseActivity,
        log: FlowLog = randomLog(),
    ) = ItemHistory(date = date, activity = activity, log = log)

    private fun randomLog() =
        FlowLog("", Log.Status.CONFIRMED, 1, "", Instant.now(Clock.systemUTC()), randomLong(), "")
}
