package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
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
import java.time.ZonedDateTime
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
    internal fun `mint-burn history events`() {
        val contract = "A.ebf4ae01d1284af8.RaribleNFT"
        val tokenId = 569L

        val date1 = ZonedDateTime.parse("2021-11-09T09:14:01.708Z").toInstant()
        val date2 = ZonedDateTime.parse("2021-11-09T09:14:36.388Z").toInstant()
        val acc1 = "0x8b3a1957d16153ed"

        val royalties = listOf(
            Part(FlowAddress("0x6f5da08ac09b5332"), 0.12),
            Part(FlowAddress("0x80102bce1de42dc4"), 0.08)
        )
        val metadata = mapOf(
            "metaURI" to "ipfs://ipfs/QmNe7Hd9xiqm1MXPtQQjVtksvWX6ieq9Wr6kgtqFo9D4CU"
        )

        val flowLog1 = FlowLog(
            "32b8e20c643740a6e96b48af96f015655801864d3dbed3f22611ee94af421f86",
            Log.Status.CONFIRMED,
            0,
            "",
            date1,
            50564298L,
            "12f9dc7ed8fc0b9043802719f13e4cd20fed6780d2d8d621284b08dc6485ba5e"
        )
        val flowLog2 = FlowLog(
            "e2b72842eb40183ce2a956d9103b29c1ce2efe013bccfdd12730c3148e550a10",
            Log.Status.CONFIRMED,
            0,
            "",
            date2,
            50564339L,
            "c452cb8a5a009447fbd7632f1e7f5af4698ba276e1cb77097b017e08874f2477"
        )
        val history = listOf(
            ItemHistory(
                date1,
                MintActivity(FlowActivityType.MINT, acc1, contract, tokenId, 1, date1, royalties, metadata),
                flowLog1.copy(eventIndex = 0, eventType = "A.ebf4ae01d1284af8.RaribleNFT.Mint")
            ),
            ItemHistory(
                date1,
                DepositActivity(FlowActivityType.DEPOSIT, contract, tokenId, date1, acc1),
                flowLog1.copy(eventIndex = 1, eventType = "A.ebf4ae01d1284af8.RaribleNFT.Deposit")
            ),
            ItemHistory(
                date2,
                WithdrawnActivity(FlowActivityType.WITHDRAWN, contract, tokenId, date2, acc1),
                flowLog2.copy(eventIndex = 0, eventType = "A.ebf4ae01d1284af8.RaribleNFT.Withdraw")
            ),
            ItemHistory(
                date2,
                BurnActivity(FlowActivityType.BURN, contract, tokenId, 1, null, date2),
                flowLog2.copy(eventIndex = 1, eventType = "A.ebf4ae01d1284af8.RaribleNFT.Destroy")
            ),
        )
        repo.saveAll(history).subscribe()

        client.get().uri("/v0.1/order/activities/byItem?type=BURN&contract=$contract&tokenId=$tokenId")
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
    internal fun `mint-transfer-burn history events`() {
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
        FlowLog(UUID.randomUUID().toString(),
            Log.Status.CONFIRMED,
            1,
            "",
            Instant.now(Clock.systemUTC()),
            randomLong(),
            "")
}
