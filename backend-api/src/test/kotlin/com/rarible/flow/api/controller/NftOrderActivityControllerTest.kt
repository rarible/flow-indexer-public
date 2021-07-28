package com.rarible.flow.api.controller

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
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
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import org.testcontainers.shaded.org.apache.commons.lang.math.RandomUtils
import java.time.Duration
import java.time.LocalDateTime
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
    fun `should return all activities by item`() {
        val expectedTokenId = RandomUtils.nextLong().toULong().toLong()
        val expectedContract = randomAddress()

        val mintActivity = MintActivity(
            owner = FlowAddress(randomAddress()),
            contract = FlowAddress(expectedContract),
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
        )

        val transferActivity = TransferActivity(
            from = FlowAddress(randomAddress()),
            owner = FlowAddress(randomAddress()),
            contract = FlowAddress(expectedContract),
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
        )

        val burnActivity = BurnActivity(
            owner = null,
            contract = FlowAddress(expectedContract),
            tokenId = expectedTokenId,
            value = RandomUtils.nextLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong(),
        )

        val history = listOf(
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), mintActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), mintActivity.copy(tokenId = RandomUtils.nextLong())),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), transferActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), burnActivity),
        )

        repo.saveAll(history).then().block()

        val activities = client.get()
            .uri("/v0.1/activities/byItem?type=&contract=${expectedContract}&tokenId=$expectedTokenId")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowActivitiesDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(activities, "Answer is NULL!")
        Assertions.assertTrue(activities.items.isNotEmpty(), "Activities list is empty!")
        Assertions.assertTrue(activities.items.size == 3, "Size of activities list is not 3!")
        Assertions.assertTrue(activities.items[0] is MintDto)
        Assertions.assertTrue(activities.items[1] is TransferDto)
        Assertions.assertTrue(activities.items[2] is BurnDto)

        val (m, t, b) = activities.items
        m as MintDto
        Assertions.assertEquals(mintActivity.contract.formatted, m.contract, "Mint activity: contracts are different!")
        Assertions.assertEquals(mintActivity.owner.formatted, m.owner, "Mint activity: owners are different!")
        Assertions.assertEquals(mintActivity.type, FlowActivityType.MINT, "Mint activity: types are different!")
        Assertions.assertEquals(mintActivity.tokenId, m.tokenId.toLong(), "Mint activity: token ids are different!")
        Assertions.assertEquals(mintActivity.value, m.value.toLong(), "Mint activity: values are different!")
        Assertions.assertEquals(mintActivity.transactionHash, m.transactionHash, "Mint activity: transactions are different!")
        Assertions.assertEquals(mintActivity.blockHash, m.blockHash, "Mint activity: blocks are different!")
        Assertions.assertEquals(mintActivity.blockNumber, m.blockNumber, "Mint activity: block numbers are different!")

        t as TransferDto
        Assertions.assertEquals(transferActivity.contract.formatted, t.contract, "Transfer activity: contracts are different!")
        Assertions.assertEquals(transferActivity.owner.formatted, t.owner, "Transfer activity: owners are different!")
        Assertions.assertEquals(transferActivity.from.formatted, t.from, "Transfer activity: froms are different!")
        Assertions.assertEquals(transferActivity.type, FlowActivityType.TRANSFER, "Transfer activity: types are different!")
        Assertions.assertEquals(transferActivity.tokenId, t.tokenId.toLong(), "Transfer activity: token ids are different!")
        Assertions.assertEquals(transferActivity.value, t.value.toLong(), "Transfer activity: values are different!")
        Assertions.assertEquals(transferActivity.transactionHash, t.transactionHash, "Transfer activity: transactions are different!")
        Assertions.assertEquals(transferActivity.blockHash, t.blockHash, "Transfer activity: blocks are different!")
        Assertions.assertEquals(transferActivity.blockNumber, t.blockNumber, "Transfer activity: block numbers are different!")


        b as BurnDto
        Assertions.assertEquals(burnActivity.contract.formatted, b.contract, "Burn activity: contracts are different!")
        Assertions.assertTrue(b.owner.isEmpty(), "Burn activity: owner is not NULL!")
        Assertions.assertEquals(burnActivity.type, FlowActivityType.BURN, "Burn activity: types are different!")
        Assertions.assertEquals(burnActivity.tokenId, b.tokenId.toLong(), "Burn activity: token ids are different!")
        Assertions.assertEquals(burnActivity.value, b.value.toLong(), "Burn activity: values are different!")
        Assertions.assertEquals(burnActivity.transactionHash, b.transactionHash, "Burn activity: transactions are different!")
        Assertions.assertEquals(burnActivity.blockHash, b.blockHash, "Burn activity: blocks are different!")
        Assertions.assertEquals(burnActivity.blockNumber, b.blockNumber, "Burn activity: block numbers are different!")

    }

    private fun randomAddress() = "0x${RandomStringUtils.random(16, "0123456789ABCDEF")}".lowercase(Locale.ENGLISH)

    @Test
    fun `should return 1 activity by item`() {
        val mintActivity = MintActivity(
            owner = FlowAddress(randomAddress()),
            contract = FlowAddress(randomAddress()),
            tokenId = RandomUtils.nextLong().toULong().toLong(),
            value = RandomUtils.nextLong().toULong().toLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong().toULong().toLong(),
        )

        val transferActivity = TransferActivity(
            from = FlowAddress(randomAddress()),
            owner = FlowAddress(randomAddress()),
            contract = FlowAddress(randomAddress()),
            tokenId = RandomUtils.nextLong().toULong().toLong(),
            value = RandomUtils.nextLong().toULong().toLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong().toULong().toLong(),
        )

        val burnActivity = BurnActivity(
            owner = FlowAddress(randomAddress()),
            contract = FlowAddress(randomAddress()),
            tokenId = RandomUtils.nextLong().toULong().toLong(),
            value = RandomUtils.nextLong().toULong().toLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong().toULong().toLong(),
        )

        val expectedTokenId = RandomUtils.nextLong().toULong().toLong()
        val expectedContract = randomAddress()
        val expected = mintActivity.copy(tokenId = expectedTokenId, contract = FlowAddress(expectedContract))

        val history = listOf(
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), mintActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), expected),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), transferActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), burnActivity),
        )

        repo.saveAll(history).then().block()

        val activities = client.get()
            .uri("/v0.1/activities/byItem?type=MINT&contract=${expectedContract}&tokenId=${expectedTokenId}")
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
        Assertions.assertEquals(expected.contract.formatted, activity.contract, "Contract's addresses are not equals!")
    }

    @Test
    fun `should return 1 activity by userFrom and 3 activities for userTo`() {
        val userFrom = FlowAddress(randomAddress())
        val userTo = FlowAddress(randomAddress())

        val mintActivity = MintActivity(
            owner = userTo,
            contract = FlowAddress(randomAddress()),
            tokenId = RandomUtils.nextLong().toULong().toLong(),
            value = RandomUtils.nextLong().toULong().toLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong().toULong().toLong(),
        )

        val transferActivity = TransferActivity(
            from = userFrom,
            owner = userTo,
            contract = FlowAddress(randomAddress()),
            tokenId = RandomUtils.nextLong().toULong().toLong(),
            value = RandomUtils.nextLong().toULong().toLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong().toULong().toLong(),
        )

        val burnActivity = BurnActivity(
            owner = userTo,
            contract = FlowAddress(randomAddress()),
            tokenId = RandomUtils.nextLong().toULong().toLong(),
            value = RandomUtils.nextLong().toULong().toLong(),
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = RandomUtils.nextLong().toULong().toLong(),
        )

        val expectedTokenId = RandomUtils.nextLong().toULong().toLong()
        val expectedContract = randomAddress()
        val expected = mintActivity.copy(tokenId = expectedTokenId, contract = FlowAddress(expectedContract), owner = userFrom)

        val history = listOf(
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now() + Duration.ofSeconds(1L), mintActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now() + Duration.ofSeconds(3L), expected),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now() + Duration.ofSeconds(6L), transferActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now() + Duration.ofSeconds(9L), burnActivity),
        )

        repo.saveAll(history).then().block()

        var activities = client.get()
            .uri("/v0.1/activities/byUser?type=TRANSFER_FROM&user=${userFrom.formatted}")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowActivitiesDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(activities)
        Assertions.assertTrue(activities.items.isNotEmpty())
        Assertions.assertTrue(activities.items.size == 1)
        Assertions.assertTrue(activities.items[0] is TransferDto)
        val item = activities.items[0] as TransferDto
        Assertions.assertEquals(item.from, userFrom.formatted, "Users is not equals!")


        activities = client.get()
            .uri("/v0.1/activities/byUser?type=MINT,BURN,TRANSFER_TO&user=${userTo.formatted}")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowActivitiesDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(activities)
        Assertions.assertTrue(activities.items.isNotEmpty())
        Assertions.assertTrue(activities.items.size == 3)

        Assertions.assertTrue(activities.items[0] is MintDto)
        Assertions.assertTrue(activities.items[1] is TransferDto)
        Assertions.assertTrue(activities.items[2] is BurnDto)

        val mint = activities.items[0] as MintDto
        val transfer = activities.items[1] as TransferDto
        val burn = activities.items[2] as BurnDto

        Assertions.assertEquals(mint.owner, mintActivity.owner.formatted, "Mint activity: owners are not equals!")
        Assertions.assertEquals(transfer.owner, transferActivity.owner.formatted, "Transfer activity: owners are not equals!")
        Assertions.assertNotEquals(burn.owner, burnActivity.owner?.formatted.orEmpty(), "Burn activity: owners are equals! Returned owner must be null!")
    }
}
