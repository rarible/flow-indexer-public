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
import java.time.Duration
import java.time.Instant
import java.util.*
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
        val contract = randomAddress()

        repo.saveAll(listOf(
            randomItemHistory(date = date, activity = randomWithdraw().copy(tokenId = tokenId, from = expectedUser, contract = contract), log = randomLog().copy(transactionHash = hash)),
            randomItemHistory(date = date, activity = randomBurn().copy(tokenId = tokenId, owner = null, contract = contract), log = randomLog().copy(transactionHash = hash, eventIndex = 2)),
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

    @Test
    internal fun test() {
        val account1 = randomAddress()
        val account2 = randomAddress()
        val contract = "A.01ab36aaf654a13e.RaribleNFT"
        val tokenId = 52L

        val date1 = Instant.now(Clock.systemUTC())
        val date2 = date1 + Duration.ofMinutes(1)
        val date3 = date1 + Duration.ofMinutes(2)


        repo.saveAll(listOf(
            // mint
            ItemHistory(
                date = date1,
                activity = randomMint().copy(
                    timestamp = date1,
                    contract = contract,
                    tokenId = tokenId,
                    owner = account1,
                ),
                log = randomLog().copy(eventIndex = 1, transactionHash = "1")
            ),
            ItemHistory(
                date = date1,
                activity = deposit(date1, contract, tokenId, account1),
                log = randomLog().copy(eventIndex = 2, transactionHash = "1")
            ),
            // transfer
            ItemHistory(
                date = date2,
                activity = randomWithdraw().copy(
                    timestamp = date2,
                    contract = contract,
                    tokenId = tokenId,
                    from = account1,
                ),
                log = randomLog().copy(eventIndex = 1, transactionHash = "2")
            ),
            ItemHistory(
                date = date2,
                activity = deposit(date1, contract, tokenId, account2),
                log = randomLog().copy(eventIndex = 2, transactionHash = "2")
            ),
            // burn
            ItemHistory(
                date = date3,
                activity = randomWithdraw().copy(
                    timestamp = date2,
                    contract = contract,
                    tokenId = tokenId,
                    from = account2,
                ),
                log = randomLog().copy(eventIndex = 1, transactionHash = "3")
            ),
            ItemHistory(
                date = date3,
                activity = randomBurn().copy(
                    timestamp = date2,
                    contract = contract,
                    tokenId = tokenId,
                    owner = null,
                ),
                log = randomLog().copy(eventIndex = 2, transactionHash = "3")
            ),
        )).then().block()

        listOf(
            "/v0.1/order/activities/byItem?type=TRANSFER&contract=$contract&tokenId=$tokenId",
            "/v0.1/order/activities/byItem?type=BURN&contract=$contract&tokenId=$tokenId",
            "/v0.1/order/activities/byItem?type=TRANSFER&contract=$contract&tokenId=$tokenId&sort=EARLIEST_FIRST",
            "/v0.1/order/activities/byItem?type=BURN&contract=$contract&tokenId=$tokenId&sort=EARLIEST_FIRST",
        ).forEach {
            client.get().uri(it)
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
    }

    private fun randomMint() = MintActivity(
        type = FlowActivityType.MINT,
        timestamp = Instant.now(Clock.systemUTC()),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomLong(),
        value = 1,
        metadata = mapOf("metaURI" to "ipfs://"),
        royalties = (0..Random.Default.nextInt(0, 3)).map { Part(randomFlowAddress(), randomRate()) },
    )

    fun randomBurn() = BurnActivity(
        type = FlowActivityType.BURN,
        timestamp = Instant.now(Clock.systemUTC()),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomLong(),
        value = 1,
    )

    fun randomWithdraw() = WithdrawnActivity(
        type = FlowActivityType.WITHDRAWN,
        timestamp = Instant.now(Clock.systemUTC()),
        from = randomAddress(),
        contract = randomAddress(),
        tokenId = randomLong(),
    )

    fun deposit(
        timestamp: Instant,
        contract: String = randomAddress(),
        tokenId: Long = randomLong(),
        to: String? = randomAddress(),
    ) = DepositActivity(FlowActivityType.DEPOSIT, contract, tokenId, timestamp, to)

    fun randomItemHistory(
        date: Instant = Instant.now(Clock.systemUTC()),
        activity: BaseActivity,
        log: FlowLog = randomLog(),
    ) = ItemHistory(date = date, activity = activity, log = log)

    private fun randomLog() =
        FlowLog(UUID.randomUUID().toString(), Log.Status.CONFIRMED, 1, "", Instant.now(Clock.systemUTC()), randomLong(), "")
}
