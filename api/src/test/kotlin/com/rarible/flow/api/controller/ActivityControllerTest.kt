package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.api.config.Config
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowNftOrderActivityBid
import com.rarible.flow.core.domain.FlowNftOrderActivityCancelList
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.OrderActivityMatchSide
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.randomAddress
import com.rarible.flow.randomFlowAddress
import com.rarible.flow.randomLong
import com.rarible.flow.randomRate
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.dto.FlowNftOrderActivityListDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
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
@Disabled("Need to rework controller tests")
class ActivityControllerTest {

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
    @Disabled("TODO Enable it after debug!!!")
    internal fun `should work burn by user`() {
        val expectedUser = "0x01658d9b94068f3c"
        val tokenId = randomLong()
        val date = Instant.now(Clock.systemUTC())
        val hash = "12345"
        val contract = randomAddress()

        repo.saveAll(
            listOf(
                randomItemHistory(
                    date = date,
                    activity = randomBurn().copy(tokenId = tokenId, owner = null, contract = contract),
                    log = randomLog().copy(transactionHash = hash, eventIndex = 2)
                ),
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
    @Disabled("TODO Enable it after debug!!!")
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
            transactionHash = "32b8e20c643740a6e96b48af96f015655801864d3dbed3f22611ee94af421f86",
            eventIndex = 0,
            eventType = "",
            timestamp = date1,
            blockHeight = 50564298L,
            blockHash = "12f9dc7ed8fc0b9043802719f13e4cd20fed6780d2d8d621284b08dc6485ba5e"
        )
        val flowLog2 = FlowLog(
            transactionHash = "e2b72842eb40183ce2a956d9103b29c1ce2efe013bccfdd12730c3148e550a10",
            eventIndex = 0,
            eventType = "",
            timestamp = date2,
            blockHeight = 50564339L,
            blockHash = "c452cb8a5a009447fbd7632f1e7f5af4698ba276e1cb77097b017e08874f2477"
        )
        val history = listOf(
            ItemHistory(
                date1,
                MintActivity(FlowActivityType.MINT, acc1, contract, tokenId, 1, date1, acc1, royalties, metadata),
                flowLog1.copy(eventIndex = 0, eventType = "A.ebf4ae01d1284af8.RaribleNFT.Mint")
            ),
            ItemHistory(
                date2,
                BurnActivity(FlowActivityType.BURN, contract, tokenId, 1, acc1, date2),
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
    @Disabled("TODO Enable it after debug!!!")
    internal fun `mint-transfer-burn history events`() {
        val account1 = randomAddress()
        val account2 = randomAddress()
        val contract = "A.01ab36aaf654a13e.RaribleNFT"
        val tokenId = 52L

        val date1 = Instant.now(Clock.systemUTC())
        val date2 = date1 + Duration.ofMinutes(1)
        val date3 = date1 + Duration.ofMinutes(2)

        repo.saveAll(
            listOf(
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
                // transfer
                ItemHistory(
                    date = date2,
                    activity = TransferActivity(
                        timestamp = date2,
                        contract = contract,
                        tokenId = tokenId,
                        from = account1,
                        to = account2,
                        purchased = false
                    ),
                    log = randomLog().copy(eventIndex = 1, transactionHash = "2")
                ),
                // burn
                ItemHistory(
                    date = date3,
                    activity = randomBurn().copy(
                        timestamp = date2,
                        contract = contract,
                        tokenId = tokenId,
                        owner = account2,
                    ),
                    log = randomLog().copy(eventIndex = 2, transactionHash = "3")
                ),
            )
        ).then().block()

        listOf(
            "/v0.1/order/activities/byItem?type=TRANSFER&contract=$contract&tokenId=$tokenId",
            "/v0.1/order/activities/byItem?type=BURN&contract=$contract&tokenId=$tokenId",
            "/v0.1/order/activities/byItem?type=TRANSFER&contract=$contract&tokenId=$tokenId&sort=EARLIEST_FIRST",
            "/v0.1/order/activities/byItem?type=BURN&contract=$contract&tokenId=$tokenId&sort=EARLIEST_FIRST",
            "/v0.1/order/activities/byUser?type=TRANSFER_FROM&user=$account1",
            "/v0.1/order/activities/byUser?type=TRANSFER_TO&user=$account2",
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

    @Test
    internal fun `unknown types should return empty`() {
        repo.saveAll(
            listOf(
                ItemHistory(
                    activity = randomMint(),
                    log = randomLog(),
                    date = Instant.now()
                ),
                ItemHistory(
                    activity = randomBurn(),
                    log = randomLog(),
                    date = Instant.now()
                )
            )
        ).then().block()

        listOf(
            "/v0.1/order/activities/byItem?type=UNSUPPORTED&contract=${randomAddress()}&tokenId=${randomLong()}",
            "/v0.1/order/activities/byUser?type=UNSUPPORTED&user=${randomAddress()}",
            "/v0.1/order/activities/byCollection?type=UNSUPPORTED&collection=A.0000000000.RandomCollection",
            "/v0.1/order/activities/all?type=UNSUPPORTED"
        ).forEach { url ->
            client.get().uri(url)
                .exchange()
                .expectStatus().isOk
                .expectBody<FlowActivitiesDto>()
                .consumeWith { response ->
                    val activitiesDto = response.responseBody
                    Assertions.assertNotNull(activitiesDto)
                    Assertions.assertNotNull(activitiesDto?.items)
                    Assertions.assertTrue(activitiesDto?.items!!.isEmpty())
                    Assertions.assertNull(activitiesDto.continuation)
                }
        }
    }

    @Test
    @Disabled("TODO Enable it after debug!!!")
    internal fun `should get bid cancel_bid`() {
        val user = "0xf60ae072502ac3e6"
        val contract = "A.01ab36aaf654a13e.RaribleNFT"
        val tokenId = 74L

        val history = listOf(
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:11:36.543Z").toInstant(),
                FlowNftOrderActivityList(
                    type = FlowActivityType.BID,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:11:36.543Z").toInstant(),
                    price = BigDecimal("9.00000000"),
                    priceUsd = BigDecimal("9.00000000"),
                    make = FlowAssetFungible(contract, BigDecimal.TEN),
                    take = FlowAssetNFT(contract, BigDecimal.ONE, tokenId),
                    maker = user,
                    hash = "79268631",
                    estimatedFee = null,
                    expiry = null
                ),
                FlowLog(
                    transactionHash = "052dd413b2fb22fb078f03b3d5cb93238376f801513a8996b45e848e92700535",
                    eventIndex = 1,
                    eventType = "A.1d56d7ba49283a88.RaribleOpenBid.BidAvailable",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:11:36.543Z").toInstant(),
                    blockHeight = 20231662L,
                    blockHash = "d3265903abde4ab6518347f40f7aa2720539ef499925fd27481c0e08a1a81824"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                FlowNftOrderActivityCancelList(
                    type = FlowActivityType.CANCEL_BID,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                    hash = "79268631",
                ),
                FlowLog(
                    transactionHash = "a1b589c6a8f969f5f219b04457f3ff214c7fdce7a87eae9176805569fab89dc6",
                    eventIndex = 0,
                    eventType = "A.1d56d7ba49283a88.RaribleOpenBid.BidCompleted",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                    blockHeight = 20231684L,
                    blockHash = "f5f672878a5d2311e5742e82f5334abcf34f2ec1df813336b68d4c64264fd8c6"
                )
            ),
        )
        repo.saveAll(history).then().block()

        val urls = listOf(
            "/v0.1/order/activities/byItem?type=BID&contract=$contract&tokenId=$tokenId",
            "/v0.1/order/activities/byUser?type=MAKE_BID&user=$user",
            "/v0.1/order/activities/byCollection?type=BID&collection=$contract",
            "/v0.1/order/activities/all?type=BID"
        )
        urls.forEach { url ->
            client.get().uri(url)
                .exchange()
                .expectStatus().isOk
                .expectBody<FlowActivitiesDto>()
                .consumeWith { response ->
                    val activitiesDto = response.responseBody
                    Assertions.assertNotNull(activitiesDto)
                    Assertions.assertNotNull(activitiesDto?.items)
                    Assertions.assertTrue(activitiesDto?.items!!.isNotEmpty())
                    Assertions.assertEquals(2, activitiesDto.items.count())
                }
        }
    }

    @Test
    @Disabled("TODO Enable it after debug!!!")
    internal fun `test get_bid`() {
        val user = "0xf60ae072502ac3e6"
        val contract = "A.01ab36aaf654a13e.RaribleNFT"
        val tokenId = 74L

        val history = listOf(
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                FlowNftOrderActivitySell(
                    type = FlowActivityType.SELL,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                    price = BigDecimal("9.00000000"),
                    priceUsd = BigDecimal("9.00000000"),
                    left = OrderActivityMatchSide(randomAddress(), FlowAssetFungible(contract, BigDecimal.TEN)),
                    right = OrderActivityMatchSide(user, FlowAssetNFT(contract, BigDecimal.ONE, tokenId)),
                    hash = "79268642",
                    payments = emptyList()
                ),
                FlowLog(
                    transactionHash = "a1b589c6a8f969f5f219b04457f3ff214c7fdce7a87eae9176805569fab89dd6",
                    eventIndex = 0,
                    eventType = "A.1d56d7ba49283a88.RaribleOpenBid.BidCompleted",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                    blockHeight = 20231684L,
                    blockHash = "f5f672878a5d2311e5742e82f5334abcf34f2ec1df813336b68d4c64264fd8d6"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                FlowNftOrderActivitySell(
                    type = FlowActivityType.SELL,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                    price = BigDecimal("9.00000000"),
                    priceUsd = BigDecimal("9.00000000"),
                    left = OrderActivityMatchSide(randomAddress(), FlowAssetFungible(contract, BigDecimal.TEN)),
                    right = OrderActivityMatchSide(user, FlowAssetNFT(contract, BigDecimal.ONE, tokenId)),
                    hash = "79268642",
                    payments = emptyList()
                ),
                FlowLog(
                    transactionHash = "a1b589c6a8f969f5f219b04457f3ff214c7fdce7a87eae9176805569fab89de6",
                    eventIndex = 0,
                    eventType = "A.1d56d7ba49283a88.RaribleOpenBid.BidCompleted",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                    blockHeight = 20231684L,
                    blockHash = "f5f672878a5d2311e5742e82f5334abcf34f2ec1df813336b68d4c64264fd8e6"
                )
            ),
        )
        repo.saveAll(history).then().block()

        val urls = listOf(
            "/v0.1/order/activities/byUser?type=GET_BID&user=$user",
        )
        urls.forEach { url ->
            client.get().uri(url)
                .exchange()
                .expectStatus().isOk
                .expectBody<FlowActivitiesDto>()
                .consumeWith { response ->
                    val activitiesDto = response.responseBody
                    Assertions.assertNotNull(activitiesDto)
                    Assertions.assertNotNull(activitiesDto?.items)
                    Assertions.assertTrue(activitiesDto?.items!!.isNotEmpty())
                    Assertions.assertEquals(2, activitiesDto.items.count())
                }
        }
    }

    @Test
    @Disabled("TODO Enable it after debug!!!")
    internal fun `check order of list cancel_list`() {
        val acc1 = "0xf60ae072502ac3e6"
        val contract = "A.01ab36aaf654a13e.RaribleNFT"
        val tokenId = 74L

        val txId = "872a4957d00d75213db4c1af6d787df977e367291e4dab1c59338d64d8a9af7e"
        val orderId1 = "79268961"
        val orderId2 = "79268986"
        val history = listOf(
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:10:46.461Z").toInstant(),
                MintActivity(
                    type = FlowActivityType.MINT,
                    owner = acc1,
                    creator = acc1,
                    contract = contract,
                    tokenId = tokenId,
                    value = 1,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:10:46.461Z").toInstant(),
                    royalties = emptyList(),
                    metadata = emptyMap(),
                ),
                FlowLog(
                    transactionHash = "5ee6a33e2b2a41e62eb5e585134231c9597a2212d14afc3de21a23d412292dc8",
                    eventIndex = 0,
                    eventType = "A.01ab36aaf654a13e.RaribleNFT.Mint",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:10:46.461Z").toInstant(),
                    blockHeight = 20231636L,
                    blockHash = "ab5ec380bc5a4a64ea0af55a2f79ccbff8d1bdb5f86eeaad2709da258fb96e26"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:11:36.543Z").toInstant(),
                FlowNftOrderActivityList(
                    type = FlowActivityType.LIST,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:11:36.543Z").toInstant(),
                    price = BigDecimal("9.00000000"),
                    priceUsd = BigDecimal("9.00000000"),
                    make = FlowAssetNFT(contract, BigDecimal.ONE, tokenId),
                    take = FlowAssetFungible(contract, BigDecimal.TEN),
                    maker = acc1,
                    hash = "79268631",
                    estimatedFee = null,
                    expiry = null
                ),
                FlowLog(
                    transactionHash = "052dd413b2fb22fb078f03b3d5cb93238376f801513a8996b45e848e92700535",
                    eventIndex = 1,
                    eventType = "A.01ab36aaf654a13e.RaribleOrder.OrderAvailable",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:11:36.543Z").toInstant(),
                    blockHeight = 20231662L,
                    blockHash = "d3265903abde4ab6518347f40f7aa2720539ef499925fd27481c0e08a1a81824"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                FlowNftOrderActivityCancelList(
                    type = FlowActivityType.CANCEL_LIST,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                    hash = "79268631",
                ),
                FlowLog(
                    transactionHash = "a1b589c6a8f969f5f219b04457f3ff214c7fdce7a87eae9176805569fab89dc6",
                    eventIndex = 0,
                    eventType = "A.01ab36aaf654a13e.RaribleOrder.OrderCancelled",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:12:24.761Z").toInstant(),
                    blockHeight = 20231684L,
                    blockHash = "f5f672878a5d2311e5742e82f5334abcf34f2ec1df813336b68d4c64264fd8c6"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:13:29.236Z").toInstant(),
                TransferActivity(
                    type = FlowActivityType.TRANSFER,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:13:29.236Z").toInstant(),
                    from = acc1,
                    to = acc1,
                    purchased = false
                ),
                FlowLog(
                    transactionHash = "792410fde65b1b9b49d0b723fe6798ae2ab056535a1060f8a0e220e2acbd1e60",
                    eventIndex = 0,
                    eventType = "A.01ab36aaf654a13e.RaribleNFT.Withdraw",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:13:29.236Z").toInstant(),
                    blockHeight = 20231715L,
                    blockHash = "d21b65206fb357d5d10fdb2c0a059400d3a36c77a4e54c436818937f011f74e7"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:22:38.747Z").toInstant(),
                FlowNftOrderActivityList(
                    type = FlowActivityType.LIST,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:22:38.747Z").toInstant(),
                    price = BigDecimal("56.00000000"),
                    priceUsd = BigDecimal("56.00000000"),
                    make = FlowAssetNFT(contract, BigDecimal.ONE, tokenId),
                    take = FlowAssetFungible(contract, BigDecimal.TEN),
                    maker = acc1,
                    hash = "79268934",
                    estimatedFee = null,
                    expiry = null
                ),
                FlowLog(
                    transactionHash = "c0b46d62cbf5086647d49269fdac82689205a708644b9a785e9ae00c2d08e1f6",
                    eventIndex = 1,
                    eventType = "A.01ab36aaf654a13e.RaribleOrder.OrderAvailable",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:22:38.747Z").toInstant(),
                    blockHeight = 20231968L,
                    blockHash = "516b0d641b13b5e105cf378b66735ba6e8e60e11b198c79adbeaa37f62863caa"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:23:22.776Z").toInstant(),
                FlowNftOrderActivityCancelList(
                    type = FlowActivityType.CANCEL_LIST,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:23:22.776Z").toInstant(),
                    hash = "79268934",
                ),
                FlowLog(
                    transactionHash = "773743a95921c169cfd9b405c2ef970871d452229751d385ca6f655a4c83091f",
                    eventIndex = 0,
                    eventType = "A.01ab36aaf654a13e.RaribleOrder.OrderCancelled",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:23:22.776Z").toInstant(),
                    blockHeight = 20231991L,
                    blockHash = "d3a19312f2f8b596eb484a78da9ba911ba673092be75a3b97bb0404c31f1998a"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:24:16.106Z").toInstant(),
                FlowNftOrderActivityList(
                    type = FlowActivityType.LIST,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:24:16.106Z").toInstant(),
                    price = BigDecimal("12.00000000"),
                    priceUsd = BigDecimal("12.00000000"),
                    make = FlowAssetNFT(contract, BigDecimal.ONE, tokenId),
                    take = FlowAssetFungible(contract, BigDecimal.TEN),
                    maker = acc1,
                    hash = orderId1,
                    estimatedFee = null,
                    expiry = null
                ),
                FlowLog(
                    transactionHash = "426b02599cd10f9e7afb92ac02bafceda2e631a38d982326c60444f968b8bbb0",
                    eventIndex = 1,
                    eventType = "A.01ab36aaf654a13e.RaribleOrder.OrderAvailable",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:24:16.106Z").toInstant(),
                    blockHeight = 20232018L,
                    blockHash = "396b0c8b7f4d9af31213195ec394cc1a4f5d022e04f196f2c5ed3c5c6b18e51f"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:25:05.165Z").toInstant(),
                FlowNftOrderActivityCancelList(
                    type = FlowActivityType.CANCEL_LIST,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:25:05.165Z").toInstant(),
                    hash = orderId1,
                ),
                FlowLog(
                    transactionHash = txId,
                    eventIndex = 0,
                    eventType = "A.01ab36aaf654a13e.RaribleOrder.OrderCancelled",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:25:05.165Z").toInstant(),
                    blockHeight = 20232039L,
                    blockHash = "6eaa99f0a203ea1448b72670743b271904f0ceb36167a893f178787cce3f1de6"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:25:05.165Z").toInstant(),
                FlowNftOrderActivityList(
                    type = FlowActivityType.LIST,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:25:05.165Z").toInstant(),
                    price = BigDecimal("10.00000000"),
                    priceUsd = BigDecimal("10.00000000"),
                    make = FlowAssetNFT(contract, BigDecimal.ONE, tokenId),
                    take = FlowAssetFungible(contract, BigDecimal.TEN),
                    maker = acc1,
                    hash = orderId2,
                    estimatedFee = null,
                    expiry = null
                ),
                FlowLog(
                    transactionHash = txId,
                    eventIndex = 3,
                    eventType = "A.01ab36aaf654a13e.RaribleOrder.OrderAvailable",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:25:05.165Z").toInstant(),
                    blockHeight = 20232039L,
                    blockHash = "6eaa99f0a203ea1448b72670743b271904f0ceb36167a893f178787cce3f1de6"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:39:13.909Z").toInstant(),
                FlowNftOrderActivityCancelList(
                    type = FlowActivityType.CANCEL_LIST,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:39:13.909Z").toInstant(),
                    hash = orderId2,
                ),
                FlowLog(
                    transactionHash = "d434fac4c05f675f34bcf624c706226b453caebe0efd03355e585691a2a6cca9",
                    eventIndex = 0,
                    eventType = "A.01ab36aaf654a13e.RaribleOrder.OrderCancelled",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:39:13.909Z").toInstant(),
                    blockHeight = 20232420L,
                    blockHash = "edffa5b4a5d653c78aa35d9cf23087b8ec02031b2bab43da779f0540207e3785"
                )
            ),
            ItemHistory(
                ZonedDateTime.parse("2021-11-10T11:40:14.676Z").toInstant(),
                BurnActivity(
                    type = FlowActivityType.BURN,
                    owner = acc1,
                    contract = contract,
                    tokenId = tokenId,
                    value = 1,
                    timestamp = ZonedDateTime.parse("2021-11-10T11:40:14.676Z").toInstant(),
                ),
                FlowLog(
                    transactionHash = "7640ec0cc8416e461740f4a7670eb7f4251bba3dad1129e867a49e12f932e18a",
                    eventIndex = 1,
                    eventType = "A.01ab36aaf654a13e.RaribleNFT.Destroy",
                    timestamp = ZonedDateTime.parse("2021-11-10T11:40:14.676Z").toInstant(),
                    blockHeight = 20232450L,
                    blockHash = "5048dbc3029ff9fcf780ee3c31d1646f7bdd7c1df79487e8253c12842e80bf8f"
                )
            ),
        )
        repo.saveAll(history).then().block()

        val url = "/v0.1/order/activities/byItem?type=LIST&type=SELL&contract=$contract&tokenId=$tokenId"
        client.get().uri(url)
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowActivitiesDto>()
            .consumeWith { response ->
                val activitiesDto = response.responseBody
                Assertions.assertNotNull(activitiesDto)
                Assertions.assertNotNull(activitiesDto?.items)
                Assertions.assertTrue(activitiesDto?.items!!.isNotEmpty())
                val i1 = activitiesDto.items.find { (it as? FlowNftOrderActivityCancelListDto)?.hash == orderId1 }
                val i2 = activitiesDto.items.find { (it as? FlowNftOrderActivityListDto)?.hash == orderId2 }
                Assertions.assertNotNull(i1)
                Assertions.assertNotNull(i2)
                Assertions.assertTrue { activitiesDto.items.indexOf(i1) > activitiesDto.items.indexOf(i2) }
            }
    }

    @Test
    fun `sync pagination returns all elements`() {
        val contract = randomAddress()
        val tokenId = randomLong()
        val user = randomAddress()
        val startDate = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)
        // 2 entries for each month
        val history = (1..10)
            .map { it % 5 }
            .sorted()
            .flatMap {
                listOf(
                    ItemHistory(
                        Instant.now(),
                        FlowNftOrderActivitySell(
                            type = FlowActivityType.SELL,
                            contract = contract,
                            tokenId = tokenId,
                            timestamp = Instant.now(),
                            price = BigDecimal(it),
                            priceUsd = BigDecimal(it),
                            left = OrderActivityMatchSide(randomAddress(), FlowAssetFungible(contract, BigDecimal.TEN)),
                            right = OrderActivityMatchSide(user, FlowAssetNFT(contract, BigDecimal.ONE, tokenId)),
                            hash = it.toString(),
                            payments = emptyList()
                        ),
                        randomLog()
                    ),
                    ItemHistory(
                        Instant.now(),
                        FlowNftOrderActivityList(
                            type = FlowActivityType.LIST,
                            contract = contract,
                            tokenId = tokenId,
                            timestamp = Instant.now(),
                            price = BigDecimal(it),
                            priceUsd = BigDecimal(it),
                            make = FlowAssetFungible(contract, BigDecimal.TEN),
                            take = FlowAssetNFT(contract, BigDecimal.ONE, tokenId),
                            hash = it.toString(),
                            maker = user,
                            estimatedFee = null,
                            expiry = null
                        ),
                        randomLog()
                    ),
                    ItemHistory(
                        Instant.now(),
                        FlowNftOrderActivityBid(
                            type = FlowActivityType.BID,
                            contract = contract,
                            tokenId = tokenId,
                            timestamp = Instant.now(),
                            price = BigDecimal(it),
                            priceUsd = BigDecimal(it),
                            make = FlowAssetFungible(contract, BigDecimal.TEN),
                            take = FlowAssetNFT(contract, BigDecimal.ONE, tokenId),
                            hash = it.toString(),
                            maker = user
                        ),
                        randomLog()
                    ),
                    ItemHistory(
                        Instant.now(),
                        MintActivity(
                            type = FlowActivityType.MINT,
                            timestamp = Instant.now(),
                            owner = user,
                            creator = user,
                            contract = contract,
                            tokenId = tokenId,
                            value = 1,
                            metadata = mapOf("metaURI" to "ipfs://"),
                            royalties = emptyList()
                        ),
                        randomLog()
                    ),
                    ItemHistory(
                        Instant.now(),
                        BurnActivity(
                            type = FlowActivityType.BURN,
                            timestamp = Instant.now(),
                            owner = user,
                            contract = contract,
                            tokenId = tokenId,
                            value = 1,
                        ),
                        randomLog()
                    ),
                    ItemHistory(
                        Instant.now(),
                        TransferActivity(
                            type = FlowActivityType.TRANSFER,
                            timestamp = Instant.now(),
                            from = user,
                            to = user,
                            contract = contract,
                            tokenId = tokenId,
                        ),
                        randomLog()
                    ),
                ).also { list -> list.forEach { ih -> ih.updatedAt = startDate.plusMonths(it.toLong()).toInstant() } }
            }
        repo.saveAll(history).then().block()
        listOf(
            FlowActivityType.LIST,
            FlowActivityType.BID,
            FlowActivityType.SELL,
            FlowActivityType.MINT,
            FlowActivityType.BURN,
            FlowActivityType.TRANSFER,
        ).forEach { type ->
            val activitiesAsc = iterateOrderActivitiesSync(type, "EARLIEST_FIRST")
            val activitiesDesc = iterateOrderActivitiesSync(type, "LATEST_FIRST")
            assertThat(activitiesAsc)
                .hasSize(10)
                .isSortedAccordingTo(Comparator.comparing(FlowActivityDto::updatedAt))
            assertThat(activitiesDesc)
                .hasSize(10)
                .isSortedAccordingTo(Comparator.comparing(FlowActivityDto::updatedAt).reversed())
        }
    }

    private fun randomMint() = MintActivity(
        type = FlowActivityType.MINT,
        timestamp = Instant.now(Clock.systemUTC()),
        owner = randomAddress(),
        creator = randomAddress(),
        contract = randomAddress(),
        tokenId = randomLong(),
        value = 1,
        metadata = mapOf("metaURI" to "ipfs://"),
        royalties = (0..Random.Default.nextInt(0, 3)).map { Part(randomFlowAddress(), randomRate()) },
    )

    private fun randomBurn() = BurnActivity(
        type = FlowActivityType.BURN,
        timestamp = Instant.now(Clock.systemUTC()),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomLong(),
        value = 1,
    )

    private fun randomItemHistory(
        date: Instant = Instant.now(Clock.systemUTC()),
        activity: BaseActivity,
        log: FlowLog = randomLog(),
    ) = ItemHistory(date = date, activity = activity, log = log)

    private fun randomLog() =
        FlowLog(
            transactionHash = UUID.randomUUID().toString(),
            eventIndex = 1,
            eventType = "",
            timestamp = Instant.now(Clock.systemUTC()),
            blockHeight = randomLong(),
            blockHash = ""
        )

    private fun iterateOrderActivitiesSync(type: FlowActivityType, sort: String): List<FlowActivityDto> {
        val res = mutableListOf<FlowActivityDto>()
        val ids = mutableSetOf<String>()
        val continuations = mutableSetOf<String>()
        var currentContinuation: String? = null
        do {
            val resp = client.get()
                .uri { ub ->
                    val _ub = ub.path("/v0.1/order/activities/sync")
                        .queryParam("type", type)
                        .queryParam("size", 1)
                        .queryParam("sort", sort)
                    if (currentContinuation != null) {
                        _ub.queryParam("continuation", currentContinuation)
                    }
                    _ub.build()
                }
                .exchange()
                .expectStatus().isOk
                .expectBody<FlowActivitiesDto>()
                .returnResult()
                .responseBody
            resp.items.forEach {
                if (ids.contains(it.id)) {
                    Assertions.fail<Unit>("Duplicated id ${it.id}")
                }
                ids.add(it.id)
            }
            res.addAll(resp.items)
            if (continuations.contains(resp.continuation!!)) {
                Assertions.fail<Unit>("Duplicated continuation ${resp.continuation}")
            }
            continuations.add(resp.continuation!!)
            currentContinuation = resp.continuation
        } while (!currentContinuation.isNullOrEmpty() && currentContinuation != "null")
        return res.toList()
    }
}
