package com.rarible.flow.core.repository

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.util.*

@MongoTest
@DataMongoTest(
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ContextConfiguration(classes = [CoreConfig::class])
@ActiveProfiles("test")
class ItemHistoryRepositoryTest {

    private val testAddress = "0x5c075acc71f2f41c"

    @Autowired
    private lateinit var repo: ItemHistoryRepository

    @BeforeEach
    internal fun setUp() {
        repo.deleteAll().block()
    }

    @Test
    fun `should create mint activity`() {
        val mintActivity = MintActivity(
            owner = FlowAddress(testAddress),
            contract = FlowAddress(testAddress),
            tokenId = 1L,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = 1000L,
        )

        val entity = ItemHistory(
            id = UUID.randomUUID().toString(),
            date = LocalDateTime.now(),
            activity = mintActivity
        )
        StepVerifier.create(repo.save(entity))
            .assertNext {
                Assertions.assertNotNull(it)
                Assertions.assertTrue(it.activity is MintActivity, "Bad activity type!")
                Assertions.assertEquals(FlowActivityType.MINT, (it.activity as MintActivity).type, "Type is incorrect!")
                Assertions.assertEquals(
                    mintActivity.owner,
                    (it.activity as MintActivity).owner,
                    "Owner's is not equals!"
                )
                Assertions.assertEquals(
                    mintActivity.contract,
                    (it.activity as MintActivity).contract,
                    "Contract's is not equals!"
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should create burn activity`() {
        val burnActivity = BurnActivity(
            owner = FlowAddress(testAddress),
            contract = FlowAddress(testAddress),
            tokenId = 1L,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = 1000L,
        )

        val entity = ItemHistory(
            id = UUID.randomUUID().toString(),
            date = LocalDateTime.now(),
            activity = burnActivity
        )
        StepVerifier.create(repo.save(entity))
            .assertNext {
                Assertions.assertNotNull(it)
                Assertions.assertTrue(it.activity is BurnActivity, "Bad activity type!")
                Assertions.assertEquals(FlowActivityType.BURN, (it.activity as BurnActivity).type, "Type is incorrect!")
                Assertions.assertEquals(
                    burnActivity.owner,
                    (it.activity as BurnActivity).owner,
                    "Owner's is not equals!"
                )
                Assertions.assertEquals(
                    burnActivity.contract,
                    (it.activity as BurnActivity).contract,
                    "Contract's is not equals!"
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should create transfer activity`() {
        val transferActivity = TransferActivity(
            from = FlowAddress(testAddress),
            owner = FlowAddress(testAddress),
            contract = FlowAddress(testAddress),
            tokenId = 1L,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = 1000L,
        )

        val entity = ItemHistory(
            id = UUID.randomUUID().toString(),
            date = LocalDateTime.now(),
            activity = transferActivity
        )
        StepVerifier.create(repo.save(entity))
            .assertNext {
                Assertions.assertNotNull(it)
                Assertions.assertTrue(it.activity is TransferActivity, "Bad activity type!")
                Assertions.assertEquals(FlowActivityType.TRANSFER, (it.activity as TransferActivity).type, "Type is incorrect!")
                Assertions.assertEquals(
                    transferActivity.owner,
                    (it.activity as TransferActivity).owner,
                    "Owner's is not equals!"
                )
                Assertions.assertEquals(
                    transferActivity.contract,
                    (it.activity as TransferActivity).contract,
                    "Contract's is not equals!"
                )
                Assertions.assertEquals(
                    transferActivity.from,
                    (it.activity as TransferActivity).from,
                    "From address is incorrect!"
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should return activities by item`() {
        val mintActivity = MintActivity(
            owner = FlowAddress(testAddress),
            contract = FlowAddress(testAddress),
            tokenId = 1L,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = 1000L,
        )

        val transferActivity = TransferActivity(
            from = FlowAddress(testAddress),
            owner = FlowAddress(testAddress),
            contract = FlowAddress(testAddress),
            tokenId = 1L,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = 1001L,
        )

        val burnActivity = BurnActivity(
            owner = FlowAddress(testAddress),
            contract = FlowAddress(testAddress),
            tokenId = 1L,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = 1002L,
        )

        val history = listOf(
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), mintActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), mintActivity.copy(tokenId = 2L)),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), transferActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), burnActivity),
        )

        repo.saveAll(history).then().block()
        StepVerifier.create(repo.getNftOrderActivitiesByItem(listOf(FlowActivityType.MINT), FlowAddress(testAddress), 1L))
            .assertNext {
                Assertions.assertNotNull(it)
                Assertions.assertEquals(FlowActivityType.MINT, it.activity.type, "Type is incorrect!")
                Assertions.assertEquals(1L, it.activity.tokenId, "TokenId is incorrect!")
                Assertions.assertTrue(it.activity is MintActivity, "Activity is not Mint Activity!")
            }
            .verifyComplete()

        StepVerifier.create(repo.getNftOrderActivitiesByItem(listOf(FlowActivityType.MINT, FlowActivityType.BURN, FlowActivityType.TRANSFER), FlowAddress(testAddress), 1L))
            .assertNext {
                Assertions.assertNotNull(it)
                Assertions.assertEquals(FlowActivityType.MINT, it.activity.type, "Type is incorrect!")
                Assertions.assertEquals(1L, it.activity.tokenId, "TokenId is incorrect!")
                Assertions.assertTrue(it.activity is MintActivity, "Activity is not Mint Activity!")
            }
            .assertNext {
                Assertions.assertNotNull(it)
                Assertions.assertEquals(FlowActivityType.TRANSFER, it.activity.type, "Type is incorrect!")
                Assertions.assertEquals(1L, it.activity.tokenId, "TokenId is incorrect!")
                Assertions.assertTrue(it.activity is TransferActivity, "Activity is not Transfer Activity!")
            }
            .assertNext {
                Assertions.assertNotNull(it)
                Assertions.assertEquals(FlowActivityType.BURN, it.activity.type, "Type is incorrect!")
                Assertions.assertEquals(1L, it.activity.tokenId, "TokenId is incorrect!")
                Assertions.assertTrue(it.activity is BurnActivity, "Activity is not Burn Activity!")
            }
            .verifyComplete()

        StepVerifier.create(repo.getNftOrderActivitiesByItem(listOf(FlowActivityType.MINT, FlowActivityType.BURN, FlowActivityType.TRANSFER), FlowAddress(testAddress), 2L))
            .assertNext {
                Assertions.assertNotNull(it)
                Assertions.assertEquals(FlowActivityType.MINT, it.activity.type, "Type is incorrect!")
                Assertions.assertEquals(2L, it.activity.tokenId, "TokenId is incorrect!")
                Assertions.assertTrue(it.activity is MintActivity, "Activity is not Mint Activity!")

            }
            .verifyComplete()

    }
}
