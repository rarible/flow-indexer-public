package com.rarible.flow.api.controller

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.api.config.Config
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.randomAddress
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowTransferDto
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
    internal fun `should return only 1 burn activity`() {
        val expectedTokenId = randomLong()
        val expectedContract = randomAddress()

        val mintActivity = MintActivity(
            owner = randomAddress(),
            contract = expectedContract,
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
            collection = "NFT"
        )

        val burnActivity = BurnActivity(
            owner = randomAddress(),
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
            .uri("/v0.1/order/activities/byItem?type=BURN&type=BID&size=1&contract=${expectedContract}&tokenId=${expectedTokenId}")
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
            owner = randomAddress(),
            contract = expectedContract,
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
            collection = "NFT"
        )

        val transferActivity = TransferActivity(
            from = randomAddress(),
            owner = randomAddress(),
            contract = expectedContract,
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
            collection = "NFT"
        )

        val burnActivity = BurnActivity(
            owner = randomAddress(),
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
        Assertions.assertTrue(activities.items[0] is FlowBurnDto)
        Assertions.assertTrue(activities.items[1] is FlowTransferDto)
        Assertions.assertTrue(activities.items[2] is FlowMintDto)

        val (b, t, m) = activities.items
        m as FlowMintDto
        Assertions.assertEquals(mintActivity.contract, m.contract, "Mint activity: contracts are different!")
        Assertions.assertEquals(mintActivity.owner, m.owner, "Mint activity: owners are different!")
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

        t as FlowTransferDto
        Assertions.assertEquals(transferActivity.contract, t.contract, "Transfer activity: contracts are different!")
        Assertions.assertEquals(transferActivity.owner, t.owner, "Transfer activity: owners are different!")
        Assertions.assertEquals(transferActivity.from, t.from, "Transfer activity: froms are different!")
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


        b as FlowBurnDto
        Assertions.assertEquals(burnActivity.contract, b.contract, "Burn activity: contracts are different!")
        Assertions.assertTrue(b.owner.isNotEmpty(), "Burn activity: owner is empty!")
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
            owner = randomAddress(),
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val transferActivity = TransferActivity(
            from = randomAddress(),
            owner = randomAddress(),
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val burnActivity = BurnActivity(
            owner = randomAddress(),
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
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(), mintActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(), expected),
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(), transferActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(), burnActivity),
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
        Assertions.assertTrue(activity is FlowMintDto)
        activity as FlowMintDto
        Assertions.assertEquals(expected.transactionHash, activity.transactionHash, "Tx hashes are not equals!")
        Assertions.assertEquals(expected.blockHash, activity.blockHash, "Block's hashes are not equals!")
        Assertions.assertEquals(expected.blockNumber, activity.blockNumber, "Block's numbers are not equals!")
        Assertions.assertEquals(expected.tokenId.toBigInteger(), activity.tokenId, "Token ids are not equals!")
        Assertions.assertEquals(expected.owner, activity.owner, "Owner's addresses are not equals!")
        Assertions.assertEquals(expected.contract, activity.contract, "Contract's addresses are not equals!")
    }

    @Test
    fun `should return 1 activity by userFrom and 2 activities for userTo`() {
        val userFrom = randomAddress()
        val userTo = randomAddress()

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

        val now = Instant.now()
        val history = listOf(
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = now + Duration.ofSeconds(1L),
                mintActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = now + Duration.ofSeconds(3L),
                expected
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = now + Duration.ofSeconds(6L),
                transferActivity
            ),
            ItemHistory(
                id = UUID.randomUUID().toString(),
                date = now + Duration.ofSeconds(9L),
                burnActivity
            ),
        )

        repo.saveAll(history).then().block()

        client.get()
            .uri(
                "/v0.1/order/activities/byUser?type={type}&user={userFrom}",
                mapOf("type" to arrayOf("TRANSFER_FROM"), "userFrom" to userFrom)
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
                Assertions.assertTrue(transfer is FlowTransferDto)
                transfer as FlowTransferDto
                Assertions.assertEquals(userFrom, transfer.from, "Wrong From user in Transfer Activity!!!")
            }

        client.get()
            .uri(
                "/v0.1/order/activities/byUser?type={type}&user={userTo}&from={from}",
                mapOf(
                    "type" to arrayOf("MINT", "BURN", "TRANSFER_TO"),
                    "userTo" to userTo,
                    "from" to history[3].date.toEpochMilli()
                )
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
                Assertions.assertEquals(2, dto.total, "Should return 2 items, but return ${dto.total} items")
                Assertions.assertEquals(dto.total, dto.items.size, "Total and items.size are different!!!")
            }
    }

    @Test
    fun `should return all activities`() {
        val userFrom = randomAddress()
        val userTo = randomAddress()

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

        Assertions.assertTrue(activities.items[0] is FlowBurnDto)
        Assertions.assertTrue(activities.items[1] is FlowTransferDto)
        Assertions.assertTrue(activities.items[2] is FlowMintDto)

        val mint = activities.items[2] as FlowMintDto
        val transfer = activities.items[1] as FlowTransferDto
        val burn = activities.items[0] as FlowBurnDto

        Assertions.assertEquals(mint.owner, mintActivity.owner, "Mint activity: owners are not equals!")
        Assertions.assertEquals(
            transfer.owner,
            transferActivity.owner,
            "Transfer activity: owners are not equals!"
        )
        Assertions.assertEquals(
            burn.owner,
            burnActivity.owner,
            "Burn activity: owners are not equals!"
        )

    }

    @Test
    fun `should return all activities by collection`() {
        val mintActivity = MintActivity(
            owner = randomAddress(),
            contract = randomAddress(),
            tokenId = randomLong(),
            value = randomLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        val transferActivity = TransferActivity(
            from = randomAddress(),
            owner = randomAddress(),
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
            maker = randomAddress(),
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
            tokenId = randomLong(),
            contract = randomAddress()
        )

        val burnActivity = BurnActivity(
            owner = randomAddress(),
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

    @Test
    fun orderActivitiesByUserTest() {
        val user = randomAddress()
        val itemId = ItemId(contract = "A.${randomAddress()}.Contract", 42L)

        val mintActivity = MintActivity(
            owner = user,
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
            maker = user,
            make = FlowAssetNFT(
                contract = itemId.contract,
                value = BigDecimal.ONE,
                tokenId = itemId.tokenId
            ),
            take = FlowAssetFungible(
                contract = randomAddress(),
                value = BigDecimal.TEN
            ),
            collection = "NFT",
            tokenId = itemId.tokenId,
            contract = itemId.contract
        )

        val sellActivity = FlowNftOrderActivitySell(
            price = BigDecimal.TEN,
            collection = "NFT",
            tokenId = itemId.tokenId,
            contract = itemId.contract,
            left = OrderActivityMatchSide(
                maker = user,
                asset = FlowAssetNFT(
                    contract = itemId.contract,
                    value = BigDecimal.ONE,
                    tokenId = itemId.tokenId
                )
            ),
            right = OrderActivityMatchSide(
                maker = randomAddress(),
                asset = FlowAssetFungible(contract = randomAddress(), BigDecimal.TEN)
            ),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong()
        )

        val burnActivity = BurnActivity(
            owner = user,
            type = FlowActivityType.BURN,
            contract = itemId.contract,
            tokenId = itemId.tokenId,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = randomLong(),
            collection = "NFT"
        )

        repo.saveAll(listOf(mintActivity, listActivity, sellActivity, burnActivity).map {
            ItemHistory(id = UUID.randomUUID().toString(), date = Instant.now(), it)
        }).then().block()

        client.get().uri("/v0.1/order/activities/byUser?type={types}&user={user}", mapOf("types" to arrayOf(FlowActivityType.LIST.name, FlowActivityType.SELL.name, FlowActivityType.BURN.name) ,"user" to user))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowActivitiesDto>()
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                val items = it.responseBody!!.items
                Assertions.assertNotNull(items)
                Assertions.assertTrue(items.isNotEmpty())
                Assertions.assertEquals(3, items.size)
                val burn = items[0]
                Assertions.assertTrue(burn is FlowBurnDto)
                burn as FlowBurnDto
                Assertions.assertEquals(user, burn.owner)
            }
    }
}
