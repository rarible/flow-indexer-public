package com.rarible.flow.api.controller

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.randomAddress
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.BurnDto
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.TransferDto
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
import org.testcontainers.shaded.org.apache.commons.lang.math.RandomUtils
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*

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
    internal fun `should return only 1 burn activity`() {
        val expectedTokenId = randomLong()
        val expectedContract = randomAddress()

        val mintActivity = MintActivity(
            owner = FlowAddress(randomAddress()),
            contract = expectedContract,
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
            collection = "NFT"
        )

        val burnActivity = BurnActivity(
            owner = null,
            contract = expectedContract,
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
            collection = "NFT"
        )

        val history = listOf(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()),
                activity = mintActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()),
                activity = burnActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()),
                activity = burnActivity.copy(tokenId = randomLong())
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()),
                activity = burnActivity.copy(collection = "Other")
            ),
        )

        repo.saveAll(history).then().block()

        client.get()
            .uri("/v0.1/order/activities/byItem?type=BURN&size=1&contract=${expectedContract}&tokenId=${expectedTokenId}")
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
    internal fun `should return all activities by item`() {
        val expectedTokenId = randomLong()
        val expectedContract = randomAddress()

        val mintActivity = MintActivity(
            owner = FlowAddress(randomAddress()),
            contract = expectedContract,
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
            collection = "NFT"
        )

        val transferActivity = TransferActivity(
            from = FlowAddress(randomAddress()),
            owner = FlowAddress(randomAddress()),
            contract = expectedContract,
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
            collection = "NFT"
        )

        val burnActivity = BurnActivity(
            owner = null,
            contract = expectedContract,
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
            collection = "NFT"
        )

        val history = listOf(
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(Clock.systemUTC()), mintActivity),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()),
                mintActivity.copy(tokenId = RandomUtils.nextLong())
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()).plusSeconds(1L),
                transferActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()).plusSeconds(10L),
                burnActivity
            ),
        )

        repo.saveAll(history).then().block()

        val activities = client.get()
            .uri("/v0.1/order/activities/byItem?type=&contract=${expectedContract}&tokenId=$expectedTokenId")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowActivitiesDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(activities, "Answer is NULL!")
        Assertions.assertTrue(activities.items.isNotEmpty(), "Activities list is empty!")
        Assertions.assertTrue(activities.items.size == 3, "Size of activities list is not 3!")
        Assertions.assertTrue(activities.items[0] is BurnDto)
        Assertions.assertTrue(activities.items[1] is TransferDto)
        Assertions.assertTrue(activities.items[2] is MintDto)

        val (b, t, m) = activities.items
        m as MintDto
        Assertions.assertEquals(mintActivity.contract, m.contract, "Mint activity: contracts are different!")
        Assertions.assertEquals(mintActivity.owner.formatted, m.owner, "Mint activity: owners are different!")
        Assertions.assertEquals(mintActivity.type, FlowActivityType.MINT, "Mint activity: types are different!")
        Assertions.assertEquals(mintActivity.tokenId, m.tokenId.toLong(), "Mint activity: token ids are different!")
        Assertions.assertEquals(mintActivity.value, m.value.toLong(), "Mint activity: values are different!")
        Assertions.assertEquals(
            mintActivity.transactionHash,
            m.transactionHash,
            "Mint activity: transactions are different!"
        )
        Assertions.assertEquals(mintActivity.blockHash, m.blockHash, "Mint activity: blocks are different!")
        Assertions.assertEquals(mintActivity.blockNumber, m.blockNumber, "Mint activity: block numbers are different!")

        t as TransferDto
        Assertions.assertEquals(transferActivity.contract, t.contract, "Transfer activity: contracts are different!")
        Assertions.assertEquals(transferActivity.owner.formatted, t.owner, "Transfer activity: owners are different!")
        Assertions.assertEquals(transferActivity.from.formatted, t.from, "Transfer activity: froms are different!")
        Assertions.assertEquals(
            transferActivity.type,
            FlowActivityType.TRANSFER,
            "Transfer activity: types are different!"
        )
        Assertions.assertEquals(
            transferActivity.tokenId,
            t.tokenId.toLong(),
            "Transfer activity: token ids are different!"
        )
        Assertions.assertEquals(transferActivity.value, t.value.toLong(), "Transfer activity: values are different!")
        Assertions.assertEquals(
            transferActivity.transactionHash,
            t.transactionHash,
            "Transfer activity: transactions are different!"
        )
        Assertions.assertEquals(transferActivity.blockHash, t.blockHash, "Transfer activity: blocks are different!")
        Assertions.assertEquals(
            transferActivity.blockNumber,
            t.blockNumber,
            "Transfer activity: block numbers are different!"
        )


        b as BurnDto
        Assertions.assertEquals(burnActivity.contract, b.contract, "Burn activity: contracts are different!")
        Assertions.assertTrue(b.owner.isEmpty(), "Burn activity: owner is not NULL!")
        Assertions.assertEquals(burnActivity.type, FlowActivityType.BURN, "Burn activity: types are different!")
        Assertions.assertEquals(burnActivity.tokenId, b.tokenId.toLong(), "Burn activity: token ids are different!")
        Assertions.assertEquals(burnActivity.value, b.value.toLong(), "Burn activity: values are different!")
        Assertions.assertEquals(
            burnActivity.transactionHash,
            b.transactionHash,
            "Burn activity: transactions are different!"
        )
        Assertions.assertEquals(burnActivity.blockHash, b.blockHash, "Burn activity: blocks are different!")
        Assertions.assertEquals(burnActivity.blockNumber, b.blockNumber, "Burn activity: block numbers are different!")

    }


    @Test
    fun `should return 1 activity by item`() {
        val mintActivity = MintActivity(
            owner = FlowAddress(randomAddress()),
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val transferActivity = TransferActivity(
            from = FlowAddress(randomAddress()),
            owner = FlowAddress(randomAddress()),
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val burnActivity = BurnActivity(
            owner = FlowAddress(randomAddress()),
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val expectedTokenId = randomLong()
        val expectedContract = randomAddress()
        val expected = mintActivity.copy(tokenId = expectedTokenId, contract = expectedContract)

        val history = listOf(
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(Clock.systemUTC()), mintActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(Clock.systemUTC()), expected),
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(Clock.systemUTC()), transferActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(Clock.systemUTC()), burnActivity),
        )

        repo.saveAll(history).then().block()

        val activities = client.get()
            .uri("/v0.1/order/activities/byItem?type=MINT&contract=${expectedContract}&tokenId=${expectedTokenId}")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowActivitiesDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(activities)
        Assertions.assertTrue(activities.items.isNotEmpty())
        Assertions.assertTrue(activities.items.size == 1)
        val activity = activities.items[0]
        Assertions.assertTrue(activity is MintDto)
        activity as MintDto
        Assertions.assertEquals(expected.transactionHash, activity.transactionHash, "Tx hashes are not equals!")
        Assertions.assertEquals(expected.blockHash, activity.blockHash, "Block's hashes are not equals!")
        Assertions.assertEquals(expected.blockNumber, activity.blockNumber, "Block's numbers are not equals!")
        Assertions.assertEquals(expected.tokenId.toString(), activity.tokenId, "Token ids are not equals!")
        Assertions.assertEquals(expected.owner.formatted, activity.owner, "Owner's addresses are not equals!")
        Assertions.assertEquals(expected.contract, activity.contract, "Contract's addresses are not equals!")
    }

    @Test
    fun `should return 1 activity by userFrom and 3 activities for userTo`() {
        val userFrom = FlowAddress(randomAddress())
        val userTo = FlowAddress(randomAddress())

        val mintActivity = MintActivity(
            owner = userTo,
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val transferActivity = TransferActivity(
            from = userFrom,
            owner = userTo,
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val burnActivity = BurnActivity(
            owner = userTo,
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val expectedTokenId = randomLong()
        val expectedContract = randomAddress()
        val expected = mintActivity.copy(tokenId = expectedTokenId, contract = expectedContract, owner = userFrom)

        val history = listOf(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(1L),
                mintActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(3L),
                expected
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(6L),
                transferActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(9L),
                burnActivity
            ),
        )

        repo.saveAll(history).then().block()

        client.get()
            .uri(
                "/v0.1/order/activities/byUser?type={type}&user={userFrom}",
                mapOf("type" to arrayOf("TRANSFER_FROM"), "userFrom" to userFrom.formatted)
            )
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowActivitiesDto>()
            .consumeWith { response ->
                Assertions.assertNotNull(response.responseBody)
                val dto = response.responseBody!!
                Assertions.assertNotNull(dto.items)
                Assertions.assertNotNull(dto.total)
                Assertions.assertEquals(1, dto.total)
                Assertions.assertNotNull(dto.continuation)

                val items = dto.items
                Assertions.assertEquals(dto.total, items.size)
                val transfer = items[0]
                Assertions.assertTrue(transfer is TransferDto)
                transfer as TransferDto
                Assertions.assertEquals(userFrom.formatted, transfer.from, "Wrong From user in Transfer Activity!!!")
            }

        client.get()
            .uri(
                "/v0.1/order/activities/byUser?type={type}&user={userTo}",
                mapOf("type" to arrayOf("MINT", "BURN", "TRANSFER_TO"), "userTo" to userTo.formatted)
            )
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowActivitiesDto>()
            .consumeWith { response ->
                Assertions.assertNotNull(response.responseBody, "Response body is NULL!!!")
                val dto = response.responseBody!!
                Assertions.assertNotNull(dto.items, "Items is NULL!!!")
                Assertions.assertNotNull(dto.continuation, "Continuation is NULL!!!")
                Assertions.assertNotNull(dto.total, "Total is NULL!!!")
                Assertions.assertEquals(3, dto.total, "Should return 3 items, but return ${dto.total} items")
                Assertions.assertEquals(dto.total, dto.items.size, "Total and items.size are different!!!")
            }

/*        var activities = client.get()
            .uri()
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowActivitiesDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(activities)
        Assertions.assertTrue(activities.items.isNotEmpty())
        Assertions.assertTrue(
            activities.items.size == 3,
            "Should return 3 items, but return ${activities.items.size} items"
        )

        Assertions.assertTrue(activities.items[0] is MintDto)
        Assertions.assertTrue(activities.items[1] is TransferDto)
        Assertions.assertTrue(activities.items[2] is BurnDto)

        val mint = activities.items[0] as MintDto
        val transfer = activities.items[1] as TransferDto
        val burn = activities.items[2] as BurnDto

        Assertions.assertEquals(mint.owner, mintActivity.owner.formatted, "Mint activity: owners are not equals!")
        Assertions.assertEquals(
            transfer.owner,
            transferActivity.owner.formatted,
            "Transfer activity: owners are not equals!"
        )
        Assertions.assertNotEquals(
            burn.owner,
            burnActivity.owner?.formatted.orEmpty(),
            "Burn activity: owners are equals! Returned owner must be null!"
        )*/
    }

    @Test
    fun `should return all activities`() {
        val userFrom = FlowAddress(randomAddress())
        val userTo = FlowAddress(randomAddress())

        val mintActivity = MintActivity(
            owner = userTo,
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val transferActivity = TransferActivity(
            from = userFrom,
            owner = userTo,
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val burnActivity = BurnActivity(
            owner = userTo,
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val history = listOf(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(1L),
                mintActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(6L),
                transferActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(9L),
                burnActivity
            ),
        )
        repo.saveAll(history).then().block()

        val activities = client.get()
            .uri("/v0.1/order/activities/all?type=")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowActivitiesDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(activities)
        Assertions.assertTrue(activities.items.isNotEmpty())
        Assertions.assertTrue(activities.items.size == 3)

        Assertions.assertTrue(activities.items[0] is BurnDto)
        Assertions.assertTrue(activities.items[1] is TransferDto)
        Assertions.assertTrue(activities.items[2] is MintDto)

        val mint = activities.items[2] as MintDto
        val transfer = activities.items[1] as TransferDto
        val burn = activities.items[0] as BurnDto

        Assertions.assertEquals(mint.owner, mintActivity.owner.formatted, "Mint activity: owners are not equals!")
        Assertions.assertEquals(
            transfer.owner,
            transferActivity.owner.formatted,
            "Transfer activity: owners are not equals!"
        )
        Assertions.assertNotEquals(
            burn.owner,
            burnActivity.owner?.formatted.orEmpty(),
            "Burn activity: owners are equals! Returned owner must be null!"
        )

    }

    @Test
    internal fun `should return all activities by collection`() {
        val mintActivity = MintActivity(
            owner = FlowAddress(randomAddress()),
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val transferActivity = TransferActivity(
            from = FlowAddress(randomAddress()),
            owner = FlowAddress(randomAddress()),
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val listActivity = FlowNftOrderActivityList(
            price = BigDecimal.TEN,
            hash = UUID.randomUUID().toString(),
            maker = FlowAddress(randomAddress()),
            make = FlowAssetNFT(
                contract = randomAddress(),
                value = BigDecimal.ONE,
                tokenId = randomLong()
            ),
            take = FlowAssetFungible(
                contract = randomAddress(),
                value = BigDecimal.TEN
            ),
            collection = "NFT",
            tokenId = randomLong()
        )

        val burnActivity = BurnActivity(
            owner = FlowAddress(randomAddress()),
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val history = listOf(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(1L),
                mintActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(6L),
                transferActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(6L),
                listActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(9L),
                burnActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = Instant.now(Clock.systemUTC()) + Duration.ofSeconds(12L),
                burnActivity.copy(collection = "NonNFT")
            ),
        )
        repo.saveAll(history).then().block()

        client.get().uri("/v0.1/order/activities/byCollection?collection=NFT&type=")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowActivitiesDto>()
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                val activities = it.responseBody!!.items

                Assertions.assertNotNull(activities)
                Assertions.assertTrue(activities.isNotEmpty())
                Assertions.assertTrue(activities.size == 4)
            }

    }
}
